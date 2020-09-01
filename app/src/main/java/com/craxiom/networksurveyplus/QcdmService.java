package com.craxiom.networksurveyplus;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.craxiom.networksurveyplus.mqtt.ConnectionState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * This service is responsible for initializing all the components necessary to activate the QCDM /dev/diag port and
 * start processing the feed that comes from it.
 *
 * @since 0.1.0
 */
public class QcdmService extends Service
{
    private final AtomicBoolean pcapLoggingEnabled = new AtomicBoolean(false);

    private final QcdmServiceBinder qcdmServiceBinder;
    private final Handler uiThreadHandler;

    private HandlerThread diagHandlerThread;
    private HandlerThread fifoReadHandlerThread;
    private Handler diagHandler;
    private Handler fifoReadHandler;

    private GpsListener gpsListener;
    private FifoReadRunnable fifoReadRunnable;
    private DiagRevealerRunnable diagRevealerRunnable;

    private final QcdmMessageProcessor qcdmMessageProcessor;
    private QcdmPcapWriter qcdmPcapWriter;

    public QcdmService()
    {
        qcdmServiceBinder = new QcdmServiceBinder();
        uiThreadHandler = new Handler(Looper.getMainLooper());
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

        initializeLocationListener();

        initializeQcdmFeed();

        updateServiceNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // If we are started at boot, then that means the MainActivity was never run.  Therefore, to ensure we
        // read and respect the auto start logging user preferences, we need to read them and start logging here.
        final boolean startedAtBoot = intent.getBooleanExtra(Constants.EXTRA_STARTED_AT_BOOT, false);
        if (startedAtBoot)
        {
            Timber.i("Received the startedAtBoot flag in the NetworkSurveyService. Reading the auto start preferences");

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            final boolean autoStartPcapLogging = preferences.getBoolean(Constants.PROPERTY_AUTO_START_PCAP_LOGGING, false);
            if (autoStartPcapLogging && !pcapLoggingEnabled.get()) togglePcapLogging(true);
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return qcdmServiceBinder;
    }

    @Override
    public void onDestroy()
    {
        Timber.i("onDestroy");

        if (qcdmPcapWriter != null) qcdmPcapWriter.close();

        // TODO setCellularScanningDone();
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
                //|| getMqttConnectionState() != ConnectionState.DISCONNECTED
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
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            final int refreshRate = preferences.getInt(Constants.PROPERTY_LOCATION_REFRESH_RATE_MS, 5000);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, refreshRate, 0f, gpsListener);
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
        if (gpsListener != null)
        {
            final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) locationManager.removeUpdates(gpsListener);
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

        fifoReadRunnable = new FifoReadRunnable(applicationContext, fifoPipeName, qcdmMessageProcessor);
        fifoReadHandler.post(fifoReadRunnable);

        diagRevealerRunnable = new DiagRevealerRunnable(applicationContext, fifoPipeName);
        diagHandler.post(diagRevealerRunnable);
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
        final String[] namedPipeCommand = {"su", "-c", "exec mknod -m=rw " + fifoPipeName + " p"};

        try
        {
            final Process process = Runtime.getRuntime().exec(namedPipeCommand);
            final int exitValue = process.waitFor();
            if (exitValue != 0)
            {
                Timber.e("exit value of creating the named pipe: %s", exitValue);
                try (BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream())))
                {
                    String stdErrLine;
                    while ((stdErrLine = stdErr.readLine()) != null)
                    {
                        Timber.e("mknod ERROR: %s", stdErrLine);
                    }
                }
            } else
            {
                Timber.d("Successfully ran the mknod command to create the named pipe");
            }
        } catch (Exception e)
        {
            Timber.e(e, "Could not execute the mknod command to create the named pipe");
        }

        return true; // FIXME Update once we get the already exists exit value
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
        final ConnectionState connectionState = ConnectionState.DISCONNECTED;// TODO mqttConnection.getConnectionState();
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

            boolean successful;

            if (enable)
            {
                try
                {
                    qcdmPcapWriter = new QcdmPcapWriter();
                    qcdmMessageProcessor.registerQcdmMessageListener(qcdmPcapWriter);
                    successful = true;
                } catch (IOException e)
                {
                    Timber.e(e, "Could not create the QCDM PCAP writer");
                    successful = false;
                }
            } else
            {
                qcdmMessageProcessor.unregisterQcdmMessageListener(qcdmPcapWriter);
                qcdmPcapWriter.close();
                successful = true;
            }

            if (successful) pcapLoggingEnabled.set(enable);

            updateServiceNotification();

            final boolean newLoggingState = pcapLoggingEnabled.get();

            return successful ? newLoggingState : null;
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always runs in the same
     * process as its clients, we don't need to deal with IPC.
     */
    public class QcdmServiceBinder extends Binder
    {
        public QcdmService getService()
        {
            return QcdmService.this;
        }
    }
}
