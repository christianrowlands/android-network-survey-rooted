package com.craxiom.networksurveyplus.ui.mqtt;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.craxiom.mqttlibrary.ui.MqttConnectionFragment;
import com.craxiom.networksurveyplus.QcdmService;

import timber.log.Timber;

/**
 * A fragment for allowing the user to connect to an MQTT broker.  This fragment handles
 * the UI portion of the connection and delegates the actual connection logic to {@link com.craxiom.networksurveyplus.QcdmService}.
 *
 * @since 0.2.0
 */
public class QcdmMqttFragment extends MqttConnectionFragment
{
    @Override
    public void initializeContext()
    {
        applicationContext = requireActivity().getApplicationContext();
    }

    @Override
    protected void startAndBindToService()
    {
        startAndBindToQcdmService();
    }

    /**
     * Start the QCDM Service (it won't start if it is already started), and then bind to the service.
     * <p>
     * Starting the service will cause the cellular records to be pulled from the Android system, and then once the
     * MQTT connection is made those cellular records will be sent over the connection to the MQTT Broker.
     */
    private void startAndBindToQcdmService()
    {
        // Start the service
        Timber.i("Binding to the QCDM Service");
        final Intent serviceIntent = new Intent(applicationContext, QcdmService.class);
        applicationContext.startService(serviceIntent);

        // Bind to the service
        ServiceConnection qcdmServiceConnection = new QcdmServiceConnection();
        final boolean bound = applicationContext.bindService(serviceIntent, qcdmServiceConnection, Context.BIND_ABOVE_CLIENT);
        Timber.i("QcdmService bound in the MqttConnectionFragment: %s", bound);
    }

    /**
     * A {@link ServiceConnection} implementation for binding to the {@link QcdmService}.
     */
    private class QcdmServiceConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder binder)
        {
            Timber.i("%s service connected", name);
            service = ((QcdmService.QcdmServiceBinder) binder).getService();
            service.registerMqttConnectionStateListener(QcdmMqttFragment.this);

            updateUiState(service.getMqttConnectionState());
        }

        @Override
        public void onServiceDisconnected(final ComponentName name)
        {
            Timber.i("%s service disconnected", name);
            service = null;
        }
    }
}
