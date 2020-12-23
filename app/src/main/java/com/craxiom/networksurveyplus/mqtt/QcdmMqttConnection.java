package com.craxiom.networksurveyplus.mqtt;

import com.craxiom.messaging.LteRrc;
import com.craxiom.messaging.LteRrcData;
import com.craxiom.mqttlibrary.connection.DefaultMqttConnection;
import com.craxiom.networksurveyplus.GpsListener;
import com.craxiom.networksurveyplus.IQcdmMessageListener;
import com.craxiom.networksurveyplus.messages.DiagCommand;
import com.craxiom.networksurveyplus.messages.QcdmMessage;
import com.craxiom.networksurveyplus.messages.QcdmMessageUtils;

import static com.craxiom.networksurveyplus.messages.QcdmConstants.LOG_LTE_RRC_OTA_MSG_LOG_C;

/**
 * Class for creating a connection to an MQTT server.
 *
 * @since 0.2.0
 */
public class QcdmMqttConnection extends DefaultMqttConnection implements IQcdmMessageListener
{
    private static final String MQTT_LTE_RRC_OTA_MESSAGE_TOPIC = "lte_ota_message";

    private final String deviceId;
    private final GpsListener gpsListener;

    public QcdmMqttConnection(String deviceId, GpsListener gpsListener)
    {
        super();
        this.deviceId = deviceId;
        this.gpsListener = gpsListener;
    }

    @Override
    public void onQcdmMessage(QcdmMessage qcdmMessage)
    {
        if (qcdmMessage.getOpCode() == DiagCommand.DIAG_LOG_F)
        {
            switch (qcdmMessage.getLogType())
            {
                case LOG_LTE_RRC_OTA_MSG_LOG_C:
                    convertAndPublishLteRrcOtaMessage(qcdmMessage);
                    break;
            }
        }
    }

    /**
     * Converts a QCDM message into an LteRrc message and publishes it to the MQTT server.
     *
     * @param qcdmMessage The received QCDM message
     */
    private void convertAndPublishLteRrcOtaMessage(QcdmMessage qcdmMessage)
    {
        final LteRrc lteRrc = QcdmMessageUtils.convertLteRrcOtaMessage(qcdmMessage, gpsListener.getLatestLocation(), deviceId);
        final LteRrcData.Builder lteRrcDataBuilder = lteRrc.getData().toBuilder();
        lteRrcDataBuilder.setDeviceSerialNumber(deviceId);

        // Set the device name to the user entered value in the MQTT connection UI (or the value provided via MDM)
        if (mqttClientId != null)
        {
            lteRrcDataBuilder.setDeviceName(mqttClientId);
        }

        publishMessage(MQTT_LTE_RRC_OTA_MESSAGE_TOPIC, lteRrc);
    }
}