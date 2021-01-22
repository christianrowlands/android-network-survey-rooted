package com.craxiom.networksurveyplus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.craxiom.mqttlibrary.IConnectionStateListener;
import com.craxiom.mqttlibrary.IMqttService;
import com.craxiom.mqttlibrary.MqttConstants;
import com.craxiom.mqttlibrary.connection.BrokerConnectionInfo;
import com.craxiom.mqttlibrary.connection.ConnectionState;
import com.craxiom.mqttlibrary.ui.AConnectionFragment;
import com.craxiom.networksurveyplus.mqtt.QcdmMqttConnection;
import com.google.common.io.ByteStreams;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * This service is responsible for initializing all the components necessary to activate the QCDM /dev/diag port and
 * start processing the feed that comes from it.
 *
 * @since 0.1.0
 */
public class QcdmService extends Service implements IConnectionStateListener, SharedPreferences.OnSharedPreferenceChangeListener, IMqttService
{
    public static final int LOCATION_REFRESH_RATE = 4_000;

    private final AtomicBoolean pcapLoggingEnabled = new AtomicBoolean(false);

    private final QcdmServiceBinder qcdmServiceBinder;

    private HandlerThread diagHandlerThread;
    private HandlerThread fifoReadHandlerThread;
    private Handler diagHandler;
    private Handler fifoReadHandler;

    private String deviceId;
    private GpsListener gpsListener;
    private BroadcastReceiver managedConfigurationListener;
    private FifoReadRunnable fifoReadRunnable;
    private DiagRevealerRunnable diagRevealerRunnable;

    private final QcdmMessageProcessor qcdmMessageProcessor;
    private QcdmPcapWriter qcdmPcapWriter;

    private QcdmMqttConnection qcdmMqttConnection;

    public QcdmService()
    {
        qcdmServiceBinder = new QcdmServiceBinder();
        qcdmMessageProcessor = new QcdmMessageProcessor();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        Timber.i("Creating the QCDM Service");

        diagHandlerThread = new HandlerThread("DiagRevealer");
        diagHandlerThread.start();
        diagHandler = new Handler(diagHandlerThread.getLooper());

        fifoReadHandlerThread = new HandlerThread("FifoRead");
        fifoReadHandlerThread.start();
        fifoReadHandler = new Handler(fifoReadHandlerThread.getLooper());

        deviceId = createDeviceId();

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);

        initializeLocationListener();

        initializeQcdmFeed(); // Must be called after initializing the location listener

        // Must register for MDM updates AFTER initializing the MQTT connection because we try to make an MQTT connection if the MDM settings change
        initializeMqttConnection();
        registerManagedConfigurationListener();

        updateServiceNotification(); // Must be called after initializing the MQTT connection
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // If we are started at boot, then we need to kick off the pcap logging.
        final boolean startedAtBoot = intent.getBooleanExtra(Constants.EXTRA_STARTED_AT_BOOT, false);
        if (startedAtBoot)
        {
            Timber.i("Received the startedAtBoot flag in the QcdmService.");
            if (!pcapLoggingEnabled.get()) togglePcapLogging(true);
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        //noinspection ReturnOfInnerClass
        return qcdmServiceBinder;
    }

    @Override
    public void onDestroy()
    {
        Timber.i("onDestroy");

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);

        unregisterManagedConfigurationListener();

        if (qcdmPcapWriter != null) qcdmPcapWriter.close();

        removeLocationListener();

        diagRevealerRunnable.shutdown();
        diagHandlerThread.quitSafely();
        diagHandler = null;

        fifoReadRunnable.shutdown();
        fifoReadHandlerThread.quitSafely();
        fifoReadHandler = null;

        shutdownNotifications();
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (qcdmPcapWriter != null)
        {
            qcdmPcapWriter.onSharedPreferenceChanged(sharedPreferences, key);
        }
    }

    /**
     * Creates the {@link QcdmMqttConnection} instance.
     *
     * @since 0.1.1
     */
    public void initializeMqttConnection()
    {
        qcdmMqttConnection = new QcdmMqttConnection(deviceId, gpsListener);
        qcdmMqttConnection.registerMqttConnectionStateListener(this);
    }

    /**
     * Add this listener to various services in this class.
     *
     * @param listener A listener concerned with the different updates that this service offers.
     */
    public void registerServiceStatusListener(IServiceStatusListener listener)
    {
        if (gpsListener != null)
        {
            gpsListener.registerLocationUpdatesListener(listener);
        }

        if (qcdmPcapWriter != null)
        {
            qcdmPcapWriter.registerRecordsLoggedListener(listener);
        }
    }

    /**
     * @param listener Removes this listener so it is no longer notified of status events.
     */
    public void unregisterServiceStatusListener(IServiceStatusListener listener)
    {
        if (gpsListener != null)
        {
            gpsListener.unregisterLocationUpdatesListener(listener);
        }

        if (qcdmPcapWriter != null)
        {
            qcdmPcapWriter.unregisterRecordsLoggedListener(listener);
        }
    }

    /**
     * Toggles the cellular logging setting.
     * <p>
     * It is possible that an error occurs while trying to enable or disable logging.  In that event null will be
     * returned indicating that logging could not be toggled.
     *
     * @param enable True if logging should be enabled, false if it should be turned off.
     * @return The new state of logging.  True if it is enabled, or false if it is disabled.  Null is returned if the
     * toggling was unsuccessful.
     */
    public Boolean togglePcapLogging(boolean enable)
    {
        synchronized (pcapLoggingEnabled)
        {
            final boolean originalLoggingState = pcapLoggingEnabled.get();
            if (originalLoggingState == enable) return originalLoggingState;

            Timber.i("Toggling cellular logging to %s", enable);

            if (enable)
            {
                try
                {
                    qcdmPcapWriter.createNewPcapFile();
                } catch (Throwable t)
                {
                    Timber.e(t, "Could not create a new pcap file to write the qcdm messages to");
                    return null;
                }
                qcdmMessageProcessor.registerQcdmMessageListener(qcdmPcapWriter);
            } else
            {
                qcdmMessageProcessor.unregisterQcdmMessageListener(qcdmPcapWriter);
                qcdmPcapWriter.close();
            }

            pcapLoggingEnabled.set(enable);

            updateServiceNotification();

            return pcapLoggingEnabled.get();
        }
    }

    /**
     * Used to check if this service is still needed.
     * <p>
     * This service is still needed if logging is enabled, if the UI is visible, or if a connection is active.  In other
     * words, if there is an active consumer of the survey records.
     *
     * @return True if there is an active consumer of the records produced by this service, false otherwise.
     */
    public boolean isBeingUsed()
    {
        return pcapLoggingEnabled.get()
                || getMqttConnectionState() != ConnectionState.DISCONNECTED
                || qcdmMessageProcessor.isBeingUsed();
    }

    public boolean isPcapLoggingEnabled()
    {
        return pcapLoggingEnabled.get();
    }

    /**
     * Registers with the Android {@link LocationManager} for location updates.
     */
    private void initializeLocationListener()
    {
        if (gpsListener != null) return;

        gpsListener = new GpsListener();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Timber.w("ACCESS_FINE_LOCATION Permission not granted for the QcdmService");
            return;
        }

        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null)
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_RATE, 0f, gpsListener);
        } else
        {
            Timber.e("The location manager was null when trying to request location updates for the QcdmService");
        }
    }

    /**
     * Removes the location listener from the Android {@link LocationManager}.
     */
    private void removeLocationListener()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Timber.w("ACCESS_FINE_LOCATION Permission not granted for the NS+ app, skipping removing the location listener.");
            return;
        }

        if (gpsListener != null)
        {
            final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) locationManager.removeUpdates(gpsListener);
        }
    }

    /**
     * Register a listener so that if the Managed Config changes we will be notified of the new config
     * and can restart the MQTT broker connection with the new parameters.
     */
    private void registerManagedConfigurationListener()
    {
        final IntentFilter restrictionsFilter = new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED);

        managedConfigurationListener = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                attemptMqttConnectWithMdmConfig(true);

                if (qcdmPcapWriter != null) qcdmPcapWriter.onMdmPreferenceChanged(context);
            }
        };

        registerReceiver(managedConfigurationListener, restrictionsFilter);
    }

    /**
     * Remove the managed configuration listener.
     */
    private void unregisterManagedConfigurationListener()
    {
        if (managedConfigurationListener != null)
        {
            try
            {
                unregisterReceiver(managedConfigurationListener);
            } catch (Exception e)
            {
                Timber.e(e, "Unable to unregister the Managed Configuration Listener when pausing the app");
            }
            managedConfigurationListener = null;
        }
    }

    /**
     * Initialize the /dev/diag port so that it starts sending QCDM messages.
     */
    private void initializeQcdmFeed()
    {
        final Context applicationContext = getApplicationContext();

        final String fifoPipeName = applicationContext.getFilesDir() + "/" + Constants.FIFO_PIPE;
        createNamedPipe(fifoPipeName);

        fifoReadRunnable = new FifoReadRunnable(fifoPipeName, qcdmMessageProcessor);
        fifoReadHandler.post(fifoReadRunnable);

        diagRevealerRunnable = new DiagRevealerRunnable(applicationContext, fifoPipeName);
        diagHandler.post(diagRevealerRunnable);

        if (qcdmPcapWriter == null)
        {
            try
            {
                qcdmPcapWriter = new QcdmPcapWriter(gpsListener);
            } catch (Exception e)
            {
                Timber.e(e, "Could not create the QCDM PCAP writer");
            }
        }
    }

    /**
     * Create the named pipe that is used as a FIFO queue. This queue is where the binary output
     * from the diag port is sent.
     *
     * @param fifoPipeName THe name of the pipe to create.
     * @return True if the FIFO queue was created or already existed, false if it could not be created.
     */
    private boolean createNamedPipe(String fifoPipeName)
    {
        boolean status = false;
        final String[] namedPipeCommand = {"su", "-c", "exec mknod -m=rw " + fifoPipeName + " p"};

        try
        {
            Path path = Paths.get(fifoPipeName);
            if (Files.exists(path) &&
                    Files.readAttributes(path, BasicFileAttributes.class).isOther())
            {
                status = true; // named pipe already exists
            } else
            {

                final Process process = new ProcessBuilder(namedPipeCommand).redirectError(ProcessBuilder.Redirect.PIPE).start();
                final int exitValue = process.waitFor();

                if (exitValue != 0)
                {
                    Timber.e("exit value of creating the named pipe: %s", exitValue);
                    Timber.e("mknod ERROR: %s", new String(ByteStreams.toByteArray(process.getErrorStream())));
                } else
                {
                    status = true;
                    Timber.d("Successfully ran the mknod command to create the named pipe");
                }
            }
        } catch (Exception e)
        {
            Timber.e(e, "Could not execute the mknod command to create the named pipe");
        }
        return status;
    }

    /**
     * A notification for this service that is started in the foreground so that we can continue to get GPS location
     * updates while the phone is locked or the app is not in the foreground.
     */
    private void updateServiceNotification()
    {
        startForeground(Constants.LOGGING_NOTIFICATION_ID, buildNotification());
    }

    /**
     * Creates a new {@link Notification} based on the current state of this service.  The returned notification can
     * then be passed on to the Android system.
     *
     * @return A {@link Notification} that represents the current state of this service (e.g. if logging is enabled).
     */
    private Notification buildNotification()
    {
        final boolean logging = pcapLoggingEnabled.get();
        final ConnectionState connectionState = qcdmMqttConnection.getConnectionState();
        final boolean mqttConnectionActive = connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING;
        final CharSequence notificationTitle = getText(R.string.network_survey_notification_title);
        final String notificationText = getNotificationText(logging, mqttConnectionActive, connectionState);

        final Intent notificationIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setOngoing(true)
                .setSmallIcon(mqttConnectionActive ? R.drawable.ic_cloud_connection : (logging ? R.drawable.ic_logging_thick : R.drawable.ic_plus))
                .setContentIntent(pendingIntent)
                .setTicker(notificationTitle)
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText));

        if (connectionState == ConnectionState.CONNECTING)
        {
            builder.setColor(getResources().getColor(R.color.connectionStatusConnecting, null));
            builder.setColorized(true);
        }

        return builder.build();
    }

    /**
     * Gets the text to use for the Network Survey Service Notification.
     *
     * @param logging              True if logging is active, false if disabled.
     * @param mqttConnectionActive True if the MQTT connection is either in a connected or reconnecting state.
     * @param connectionState      The actual connection state of the MQTT broker connection.
     * @return The text that can be added to the service notification.
     * @since 0.1.1
     */
    private String getNotificationText(boolean logging, boolean mqttConnectionActive, ConnectionState connectionState)
    {
        String notificationText = "";

        if (logging)
        {
            notificationText = String.valueOf(getText(R.string.logging_notification_text)) + (mqttConnectionActive ? getText(R.string.and) : "");
        }

        switch (connectionState)
        {
            case CONNECTED:
                notificationText += getText(R.string.mqtt_connection_notification_text);
                break;
            case CONNECTING:
                notificationText += getText(R.string.mqtt_reconnecting_notification_text);
                break;
            default:
        }

        return notificationText;
    }

    /**
     * Close out the notification since we no longer need this service.
     */
    private void shutdownNotifications()
    {
        stopForeground(true);
    }

    /**
     * Gets the current MQTT connection state.
     *
     * @since 0.2.0
     */
    @Override
    public ConnectionState getMqttConnectionState()
    {
        if (qcdmMqttConnection != null) return qcdmMqttConnection.getConnectionState();

        return ConnectionState.DISCONNECTED;
    }

    /**
     * Adds an {@link com.craxiom.mqttlibrary.IConnectionStateListener} so that it will be notified of all future MQTT connection state changes.
     *
     * @param connectionStateListener The listener to add.
     * @since 0.2.0
     */
    @Override
    public void registerMqttConnectionStateListener(IConnectionStateListener connectionStateListener)
    {
        qcdmMqttConnection.registerMqttConnectionStateListener(connectionStateListener);
    }

    /**
     * Removes an {@link IConnectionStateListener} so that it will no longer be notified of MQTT connection state changes.
     *
     * @param connectionStateListener The listener to remove.
     * @since 0.2.0
     */
    @Override
    public void unregisterMqttConnectionStateListener(IConnectionStateListener connectionStateListener)
    {
        qcdmMqttConnection.unregisterMqttConnectionStateListener(connectionStateListener);
    }

    /**
     * Connect to an MQTT broker.
     *
     * @param connectionInfo The information needed to connect to the MQTT broker.
     * @since 0.2.0
     */
    @Override
    public void connectToMqttBroker(BrokerConnectionInfo connectionInfo)
    {
        qcdmMqttConnection.connect(getApplicationContext(), connectionInfo);

        qcdmMessageProcessor.registerQcdmMessageListener(qcdmMqttConnection);
    }

    /**
     * Disconnects from an MQTT broker.
     *
     * @since 0.2.0
     */
    @Override
    public void disconnectFromMqttBroker()
    {
        Timber.i("Disconnecting from the MQTT Broker");

        qcdmMessageProcessor.unregisterQcdmMessageListener(qcdmMqttConnection);

        qcdmMqttConnection.disconnect();
    }

    /**
     * Attempt an MQTT connection with configuration values provided by MDM control.
     *
     * @param forceDisconnect True, if a forced disconnect is desired.
     * @since 0.2.0
     */
    @Override
    public void attemptMqttConnectWithMdmConfig(boolean forceDisconnect)
    {
        if (isMqttMdmOverrideEnabled())
        {
            Timber.i("The MQTT MDM override is enabled, so no MDM configured MQTT connection will be attempted");
            return;
        }

        final BrokerConnectionInfo connectionInfo = getMdmBrokerConnectionInfo();

        if (connectionInfo != null)
        {
            // Make sure there is not another connection active first, if there is, disconnect. Don't use the
            // disconnectFromMqttBroker() method because it will cause the listener to get unregistered, which will
            // cause the QcdmService to get stopped if it is the last listener/user of the service.  Since we
            // are starting the connection right back up there is not a need to remove the listener.
            qcdmMqttConnection.disconnect();

            connectToMqttBroker(connectionInfo);
        } else
        {
            Timber.i("Skipping the MQTT connection because no MDN MQTT broker configuration has been set");

            if (forceDisconnect) disconnectFromMqttBroker();
        }
    }

    /**
     * @return True if the MQTT MDM override has been enabled by the user.  False if the MDM config should still be employed.
     * @since 0.2.0
     */
    private boolean isMqttMdmOverrideEnabled()
    {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return preferences.getBoolean(MqttConstants.PROPERTY_MQTT_MDM_OVERRIDE, false);
    }

    /**
     * Get the MDM configured MQTT broker connection information to use to establish the connection.
     * <p>
     * If the user has specified to override the MDM connection config, then null is returned.
     *
     * @return The connection settings to use for the MQTT broker, or null if no connection information is present or
     * the user has overrode the MDM config.
     * @since 0.2.0
     */
    private BrokerConnectionInfo getMdmBrokerConnectionInfo()
    {
        final RestrictionsManager restrictionsManager = (RestrictionsManager) getSystemService(Context.RESTRICTIONS_SERVICE);
        if (restrictionsManager != null)
        {
            final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();

            final boolean hasBrokerHost = mdmProperties.containsKey(MqttConstants.PROPERTY_MQTT_CONNECTION_HOST);
            if (!hasBrokerHost) return null;

            final String mqttBrokerHost = mdmProperties.getString(MqttConstants.PROPERTY_MQTT_CONNECTION_HOST);
            final int portNumber = mdmProperties.getInt(MqttConstants.PROPERTY_MQTT_CONNECTION_PORT, MqttConstants.DEFAULT_MQTT_PORT);
            final boolean tlsEnabled = mdmProperties.getBoolean(MqttConstants.PROPERTY_MQTT_CONNECTION_TLS_ENABLED, MqttConstants.DEFAULT_MQTT_TLS_SETTING);
            final String clientId = mdmProperties.getString(MqttConstants.PROPERTY_MQTT_CLIENT_ID);
            final String username = mdmProperties.getString(MqttConstants.PROPERTY_MQTT_USERNAME);
            final String password = mdmProperties.getString(MqttConstants.PROPERTY_MQTT_PASSWORD);

            if (mqttBrokerHost == null || clientId == null)
            {
                return null;
            }

            return new BrokerConnectionInfo(mqttBrokerHost, portNumber, tlsEnabled, clientId, username, password);
        }

        return null;
    }

    /**
     * @param newConnectionState the new Connection State.
     * @since 0.2.0
     */
    @Override
    public void onConnectionStateChange(ConnectionState newConnectionState)
    {
        updateServiceNotification();
    }

    /**
     * Class used for the client Binder.  Because we know this service always runs in the same
     * process as its clients, we don't need to deal with IPC.
     */
    public class QcdmServiceBinder extends AConnectionFragment.ServiceBinder
    {
        @Override
        public QcdmService getService()
        {
            return QcdmService.this;
        }
    }

    /**
     * @return The Android ID associated with this device and app.
     */
    @SuppressLint("HardwareIds")
    private String createDeviceId()
    {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
