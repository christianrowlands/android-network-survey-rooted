package com.craxiom.networksurveyplus.mqtt;

import com.craxiom.mqttlibrary.connection.AMqttConnection;
import com.craxiom.networksurveyplus.IQcdmMessageListener;
import com.craxiom.networksurveyplus.messages.QcdmMessage;

/**
 * Class for creating a connection to an MQTT server.
 *
 * @since 0.2.0
 */
public class QcdmMqttConnection extends AMqttConnection implements IQcdmMessageListener
{
    private static final String MQTT_LTE_RRC_OTA_MESSAGE_TOPIC = "lte_rrc_ota_message";

    public QcdmMqttConnection()
    {
        super();
    }

    @Override
    public void onQcdmMessage(QcdmMessage qcdmMessage)
    {
        // Set the device name to the user entered value in the MQTT connection UI (or the value provided via MDM)
        if (mqttClientId != null)
        {
            // TODO build real LTE_RCC_OTA message
        }

        // TODO publish message
        publishMessage(null, null);
    }

//    @Override
//    public void onGsmSurveyRecord(GsmRecord gsmRecord)
//    {
//
//        if (mqttClientId != null)
//        {
//            final GsmRecord.Builder recordBuilder = gsmRecord.toBuilder();
//            gsmRecord = recordBuilder.setData(recordBuilder.getDataBuilder().setDeviceName(mqttClientId)).build();
//        }
//
//        publishMessage(MQTT_GSM_MESSAGE_TOPIC, gsmRecord);
//    }
}