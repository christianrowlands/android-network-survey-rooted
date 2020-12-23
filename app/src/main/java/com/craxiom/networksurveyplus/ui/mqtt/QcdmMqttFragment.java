package com.craxiom.networksurveyplus.ui.mqtt;

import android.content.Context;

import com.craxiom.mqttlibrary.ui.DefaultConnectionFragment;
import com.craxiom.networksurveyplus.QcdmService;

/**
 * A fragment for allowing the user to connect to an MQTT broker.  This fragment handles
 * the UI portion of the connection and delegates the actual connection logic to {@link com.craxiom.networksurveyplus.QcdmService}.
 *
 * @since 0.2.0
 */
public class QcdmMqttFragment extends DefaultConnectionFragment<QcdmService.QcdmServiceBinder>
{
    @Override
    protected Context getApplicationContext()
    {
        return requireActivity().getApplicationContext();
    }

    @Override
    protected Class<?> getServiceClass()
    {
        return QcdmService.class;
    }
}
