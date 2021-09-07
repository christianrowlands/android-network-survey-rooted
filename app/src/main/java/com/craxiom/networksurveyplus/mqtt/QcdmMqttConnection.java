package com.craxiom.networksurveyplus.mqtt;

import android.location.Location;

import com.craxiom.messaging.GsmSignaling;
import com.craxiom.messaging.GsmSignalingData;
import com.craxiom.messaging.LteNas;
import com.craxiom.messaging.LteNasData;
import com.craxiom.messaging.LteRrc;
import com.craxiom.messaging.LteRrcData;
import com.craxiom.messaging.UmtsNas;
import com.craxiom.messaging.UmtsNasData;
import com.craxiom.messaging.WcdmaRrc;
import com.craxiom.messaging.WcdmaRrcData;
import com.craxiom.mqttlibrary.connection.DefaultMqttConnection;
import com.craxiom.networksurveyplus.BuildConfig;
import com.craxiom.networksurveyplus.GpsListener;
import com.craxiom.networksurveyplus.IPcapMessageListener;
import com.craxiom.networksurveyplus.messages.CraxiomConstants;
import com.craxiom.networksurveyplus.messages.PcapMessage;
import com.craxiom.networksurveyplus.util.ParserUtils;
import com.google.protobuf.ByteString;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import timber.log.Timber;

/**
 * Class for creating a connection to an MQTT server.
 *
 * @since 0.2.0
 */
public class QcdmMqttConnection extends DefaultMqttConnection implements IPcapMessageListener
{
    private static final String MQTT_CELLULAR_OTA_MESSAGE_TOPIC = "cellular_ota_message";
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
    public void onPcapMessage(PcapMessage pcapMessage)
    {
        switch (pcapMessage.getMessageType())
        {
            case CraxiomConstants.GSM_SIGNALING_MESSAGE_TYPE:
                convertAndPublishGsmMessage(pcapMessage);
                break;
            case CraxiomConstants.UMTS_NAS_MESSAGE_TYPE:
                convertAndPublishUmtsMessage(pcapMessage);
                break;
            case CraxiomConstants.WCDMA_RRC_MESSAGE_TYPE:
                convertAndPublishWcdmaRRCMessage(pcapMessage);
                break;
            case CraxiomConstants.LTE_RRC_MESSAGE_TYPE:
                convertAndPublishLteRrcMessage(pcapMessage);
                break;
            case CraxiomConstants.LTE_NAS_MESSAGE_TYPE:
                convertAndPublishLteNasMessage(pcapMessage);
                break;
            default:
                Timber.w("Unhandled message type for the MQTT Connection %s", pcapMessage.getMessageType());
        }
    }

    /**
     * Converts a PCAP message into a GSM Signaling message and publishes it to the MQTT server.
     *
     * @param pcapMessage The PCAP message to convert and publish.
     */
    private void convertAndPublishGsmMessage(PcapMessage pcapMessage)
    {
        final GsmSignaling gsmSignaling = convertGsmMessage(pcapMessage, gpsListener.getLatestLocation());
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, gsmSignaling);
    }

    /**
     * Converts a PCAP message into a UMTS NAS message and publishes it to the MQTT server.
     *
     * @param pcapMessage The PCAP message to convert and publish.
     */
    private void convertAndPublishUmtsMessage(PcapMessage pcapMessage)
    {
        final UmtsNas umtsNas = convertUmtsNasMessage(pcapMessage, gpsListener.getLatestLocation());
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, umtsNas);
    }

    /**
     * Converts a PCAP message into a WCDMA RRC message and publishes it to the MQTT server.
     *
     * @param pcapMessage The PCAP message to convert and publish.
     */
    private void convertAndPublishWcdmaRRCMessage(PcapMessage pcapMessage)
    {
        final WcdmaRrc wcdmaRrc = convertWcdmaRrcOtaMessage(pcapMessage, gpsListener.getLatestLocation());
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, wcdmaRrc);
    }

    /**
     * Converts a PCAP message into an LteRrc message and publishes it to the MQTT server.
     *
     * @param pcapMessage The PCAP message to convert and publish.
     */
    private void convertAndPublishLteRrcMessage(PcapMessage pcapMessage)
    {
        final LteRrc lteRrc = convertLteRrcMessage(pcapMessage, gpsListener.getLatestLocation());
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, lteRrc);
    }

    /**
     * Converts a PCAP message into an LteNas message and publishes it to the MQTT server.
     *
     * @param pcapMessage The PCAP message to convert and publish.
     */
    private void convertAndPublishLteNasMessage(PcapMessage pcapMessage)
    {
        final LteNas lteNas = convertLteNasMessage(pcapMessage, gpsListener.getLatestLocation());
        publishMessage(MQTT_CELLULAR_OTA_MESSAGE_TOPIC, lteNas);
    }

    /**
     * Converts a PCAP message to a GSM Signaling message so that it can be sent out via MQTT.
     *
     * @param pcapMessage The PCAP message to convert.
     * @param location    The location to add to the message.
     * @return The Network Survey Messaging API defined message.
     */
    private GsmSignaling convertGsmMessage(PcapMessage pcapMessage, Location location)
    {
        final GsmSignaling.Builder builder = GsmSignaling.newBuilder();
        final GsmSignalingData.Builder dataBuilder = GsmSignalingData.newBuilder();

        builder.setVersion(BuildConfig.MESSAGING_API_VERSION);
        builder.setMessageType(pcapMessage.getMessageType());

        dataBuilder.setDeviceSerialNumber(deviceId);
        if (mqttClientId != null) dataBuilder.setDeviceName(mqttClientId);
        dataBuilder.setMissionId(missionId);
        dataBuilder.setDeviceTime(ParserUtils.getRfc3339String(ZonedDateTime.now()));
        dataBuilder.setAltitude((float) location.getAltitude());
        dataBuilder.setLatitude(location.getLatitude());
        dataBuilder.setLongitude(location.getLongitude());

        dataBuilder.setPcapRecord(ByteString.copyFrom(pcapMessage.getPcapRecord()));

        dataBuilder.setChannelTypeValue(pcapMessage.getChannelType() + 1); // Here we offset by 1 to match with the GsmSignalingChannelType values

        builder.setData(dataBuilder.build());

        return builder.build();
    }

    /**
     * Converts a PCAP message to a UMTS NAS message so that it can be sent out via MQTT.
     *
     * @param pcapMessage The PCAP message to convert.
     * @param location    The location to add to the message.
     * @return The Network Survey Messaging API defined message.
     */
    public UmtsNas convertUmtsNasMessage(PcapMessage pcapMessage, Location location)
    {
        final UmtsNasData.Builder umtsNasDataBuilder = UmtsNasData.newBuilder();

        umtsNasDataBuilder.setDeviceSerialNumber(deviceId);
        if (mqttClientId != null) umtsNasDataBuilder.setDeviceName(mqttClientId);
        umtsNasDataBuilder.setMissionId(missionId);
        umtsNasDataBuilder.setDeviceTime(ParserUtils.getRfc3339String(ZonedDateTime.now()));
        umtsNasDataBuilder.setAltitude((float) location.getAltitude());
        umtsNasDataBuilder.setLatitude(location.getLatitude());
        umtsNasDataBuilder.setLongitude(location.getLongitude());
        umtsNasDataBuilder.setPcapRecord(ByteString.copyFrom(pcapMessage.getPcapRecord()));

        final UmtsNas.Builder umtsNasBuilder = UmtsNas.newBuilder();
        umtsNasBuilder.setVersion(BuildConfig.MESSAGING_API_VERSION);
        umtsNasBuilder.setMessageType(pcapMessage.getMessageType());
        umtsNasBuilder.setData(umtsNasDataBuilder.build());

        return umtsNasBuilder.build();
    }

    /**
     * Converts a PCAP message to a WCDMA RRC message so that it can be sent out via MQTT.
     *
     * @param pcapMessage The PCAP message to convert.
     * @param location    The location to add to the message.
     * @return The Network Survey Messaging API defined message.
     */
    public WcdmaRrc convertWcdmaRrcOtaMessage(PcapMessage pcapMessage, Location location)
    {
        final WcdmaRrc.Builder wcdmaRrcBuilder = WcdmaRrc.newBuilder();
        final WcdmaRrcData.Builder wcdmaRrcDataBuilder = WcdmaRrcData.newBuilder();

        wcdmaRrcBuilder.setVersion(BuildConfig.MESSAGING_API_VERSION);
        wcdmaRrcBuilder.setMessageType(pcapMessage.getMessageType());

        wcdmaRrcDataBuilder.setDeviceSerialNumber(deviceId);
        if (mqttClientId != null) wcdmaRrcDataBuilder.setDeviceName(mqttClientId);
        wcdmaRrcDataBuilder.setMissionId(missionId);
        wcdmaRrcDataBuilder.setDeviceTime(ParserUtils.getRfc3339String(ZonedDateTime.now()));
        wcdmaRrcDataBuilder.setAltitude((float) location.getAltitude());
        wcdmaRrcDataBuilder.setLatitude(location.getLatitude());
        wcdmaRrcDataBuilder.setLongitude(location.getLongitude());

        wcdmaRrcDataBuilder.setPcapRecord(ByteString.copyFrom(pcapMessage.getPcapRecord()));

        wcdmaRrcDataBuilder.setChannelTypeValue(pcapMessage.getChannelType() + 1); // Here we offset by 1 to match with the WcdmaRrcChannelType values
        wcdmaRrcBuilder.setData(wcdmaRrcDataBuilder.build());

        return wcdmaRrcBuilder.build();
    }

    /**
     * Converts a PCAP message to an LTE RRC message so that it can be sent out via MQTT.
     *
     * @param pcapMessage The PCAP message to convert.
     * @param location    The location to add to the message.
     * @return The Network Survey Messaging API defined message.
     */
    private LteRrc convertLteRrcMessage(PcapMessage pcapMessage, Location location)
    {
        final LteRrc.Builder lteRrcBuilder = LteRrc.newBuilder();
        final LteRrcData.Builder lteRrcDataBuilder = LteRrcData.newBuilder();

        lteRrcBuilder.setVersion(BuildConfig.MESSAGING_API_VERSION);
        lteRrcBuilder.setMessageType(pcapMessage.getMessageType());

        lteRrcDataBuilder.setDeviceSerialNumber(deviceId);
        if (mqttClientId != null) lteRrcDataBuilder.setDeviceName(mqttClientId);
        lteRrcDataBuilder.setMissionId(missionId);
        lteRrcDataBuilder.setDeviceTime(ParserUtils.getRfc3339String(ZonedDateTime.now()));
        lteRrcDataBuilder.setAltitude((float) location.getAltitude());
        lteRrcDataBuilder.setLatitude(location.getLatitude());
        lteRrcDataBuilder.setLongitude(location.getLongitude());

        lteRrcDataBuilder.setPcapRecord(ByteString.copyFrom(pcapMessage.getPcapRecord()));

        lteRrcDataBuilder.setChannelTypeValue(pcapMessage.getChannelType() + 1); // Here we offset by 1 to match with the LteRrcChannelType values

        lteRrcBuilder.setData(lteRrcDataBuilder.build());

        return lteRrcBuilder.build();
    }

    /**
     * Converts a PCAP message to an LTE NAS message so that it can be sent out via MQTT.
     *
     * @param pcapMessage The PCAP message to convert.
     * @param location    The location to add to the message.
     * @return The Network Survey Messaging API defined message.
     */
    public LteNas convertLteNasMessage(PcapMessage pcapMessage, Location location)
    {
        final LteNasData.Builder lteNasDataBuilder = LteNasData.newBuilder();

        lteNasDataBuilder.setDeviceSerialNumber(deviceId);
        if (mqttClientId != null) lteNasDataBuilder.setDeviceName(mqttClientId);
        lteNasDataBuilder.setMissionId(missionId);
        lteNasDataBuilder.setDeviceTime(ParserUtils.getRfc3339String(ZonedDateTime.now()));
        lteNasDataBuilder.setAltitude((float) location.getAltitude());
        lteNasDataBuilder.setLatitude(location.getLatitude());
        lteNasDataBuilder.setLongitude(location.getLongitude());
        lteNasDataBuilder.setPcapRecord(ByteString.copyFrom(pcapMessage.getPcapRecord()));

        lteNasDataBuilder.setChannelTypeValue(pcapMessage.getChannelType() + 1); // Here we offset by 1 to match with the LteNasChannelType values

        final LteNas.Builder lteNasBuilder = LteNas.newBuilder();
        lteNasBuilder.setVersion(BuildConfig.MESSAGING_API_VERSION);
        lteNasBuilder.setMessageType(pcapMessage.getMessageType());
        lteNasBuilder.setData(lteNasDataBuilder.build());

        return lteNasBuilder.build();
    }
}