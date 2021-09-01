package com.craxiom.networksurveyplus.mqtt;

import com.craxiom.messaging.LteNas;
import com.craxiom.messaging.LteRrc;
import com.craxiom.messaging.UmtsNas;
import com.craxiom.messaging.WcdmaRrc;
import com.craxiom.messaging.UmtsNasDsds;
import com.craxiom.mqttlibrary.connection.DefaultMqttConnection;
import com.craxiom.networksurveyplus.GpsListener;
import com.craxiom.networksurveyplus.IQcdmMessageListener;
import com.craxiom.networksurveyplus.messages.DiagCommand;
import com.craxiom.networksurveyplus.messages.QcdmConstants;
import com.craxiom.networksurveyplus.messages.QcdmLteParser;
import com.craxiom.networksurveyplus.messages.QcdmMessage;
import com.craxiom.networksurveyplus.messages.QcdmUmtsParser;
import com.craxiom.networksurveyplus.messages.QcdmWcdmaParser;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import timber.log.Timber;

/**
 * Class for creating a connection to an MQTT server.
 *
 * @since 0.2.0
 */
public class QcdmMqttConnection extends DefaultMqttConnection implements IQcdmMessageListener
{
    private static final String MQTT_CELLULAR_OTA_MESSAGE_TOPIC = "lte_ota_message";
    private static final String MISSION_ID_PREFIX = "NS+ ";

    private final String deviceId;
    private final GpsListener gpsListener;
    private final String missionId;

    public QcdmMqttConnection(String deviceId, GpsListener gpsListener)
    {
        super();
        this.deviceId = deviceId;
        this.gpsListener = gpsListener;
        missionId = MISSION_ID_PREFIX + deviceId + " " + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault()).format(LocalDateTime.now());
    }

    @Override
    public void onQcdmMessage(QcdmMessage qcdmMessage)
    {
        if (qcdmMessage.getOpCode() == DiagCommand.DIAG_LOG_F)
        {
            switch (qcdmMessage.getLogType())
            {
                case QcdmConstants.LOG_LTE_RRC_OTA_MSG_LOG_C:
                    convertAndPublishLteRrcMessage(qcdmMessage);
                    break;
                case QcdmConstants.UMTS_NAS_OTA:
                    convertAndPublishUmtsMessage(qcdmMessage);
                    break;
                case QcdmConstants.WCDMA_SIGNALING_MESSAGES:
                    convertAndPublishWcdmaRRCMessage(qcdmMessage);
                    break;
                case QcdmConstants.UMTS_NAS_OTA_DSDS:
                    convertAndPublishUmtsDsdsMessage(qcdmMessage);
                    break;
                case QcdmConstants.LOG_LTE_NAS_EMM_OTA_IN_MSG:
                case QcdmConstants.LOG_LTE_NAS_EMM_OTA_OUT_MSG:
                case QcdmConstants.LOG_LTE_NAS_ESM_OTA_IN_MSG:
                case QcdmConstants.LOG_LTE_NAS_ESM_OTA_OUT_MSG:
                case QcdmConstants.LOG_LTE_NAS_EMM_SEC_OTA_IN_MSG:
                case QcdmConstants.LOG_LTE_NAS_EMM_SEC_OTA_OUT_MSG:
                case QcdmConstants.LOG_LTE_NAS_ESM_SEC_OTA_IN_MSG:
                case QcdmConstants.LOG_LTE_NAS_ESM_SEC_OTA_OUT_MSG:
                    convertAndPublishLteNasMessage(qcdmMessage);
                    break;
            }
        }
    }

    /**
     * Converts a QCDM message into an LteRrc message and publishes it to the MQTT server.
     *
     * @param qcdmMessage The received QCDM message
     */
    private void convertAndPublishLteRrcMessage(QcdmMessage qcdmMessage)
    {
        final LteRrc lteRrc = QcdmLteParser.convertLteRrcOtaMessage(qcdmMessage, gpsListener.getLatestLocation(), deviceId, missionId, mqttClientId);
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, lteRrc);
    }


    private void convertAndPublishWcdmaRRCMessage(QcdmMessage qcdmMessage)
    {
        final WcdmaRrc wcdmaRrc = QcdmWcdmaParser.convertWcdmaRrcOtaMessage(qcdmMessage, gpsListener.getLatestLocation(), deviceId, missionId, mqttClientId);
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, wcdmaRrc);
    }


    private void convertAndPublishUmtsMessage(QcdmMessage qcdmMessage)
    {
        final UmtsNas umtsNas = QcdmUmtsParser.convertUmtsNasMessage(qcdmMessage, gpsListener.getLatestLocation(), deviceId, missionId, mqttClientId);
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, umtsNas);
    }

    private void convertAndPublishUmtsDsdsMessage(QcdmMessage qcdmMessage)
    {
        final UmtsNas umtsNasDsds = QcdmUmtsParser.convertUmtsNasDsdsMessage(qcdmMessage, gpsListener.getLatestLocation(), deviceId, missionId, mqttClientId);
        Timber.v("Got to the publish message for UMTS DSDS");
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, umtsNasDsds);
    }

    /**
     * Converts a QCDM message into an LteNas message and publishes it to the MQTT server.
     *
     * @param qcdmMessage The received QCDM message
     */
    private void convertAndPublishLteNasMessage(QcdmMessage qcdmMessage)
    {
        final LteNas lteNas = QcdmLteParser.convertLteNasMessage(qcdmMessage, gpsListener.getLatestLocation(), deviceId, missionId, mqttClientId);
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, lteNas);
    }
}