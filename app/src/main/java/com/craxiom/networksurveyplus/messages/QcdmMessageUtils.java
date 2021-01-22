package com.craxiom.networksurveyplus.messages;

import android.location.Location;

import com.craxiom.messaging.LteNas;
import com.craxiom.messaging.LteNasChannelType;
import com.craxiom.messaging.LteNasData;
import com.craxiom.messaging.LteRrc;
import com.craxiom.messaging.LteRrcData;
import com.craxiom.networksurveyplus.BuildConfig;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import timber.log.Timber;

import static com.craxiom.networksurveyplus.NetworkSurveyUtils.doubleToFixed37;
import static com.craxiom.networksurveyplus.NetworkSurveyUtils.doubleToFixed64;
import static com.craxiom.networksurveyplus.messages.QcdmConstants.LOG_LTE_NAS_EMM_OTA_IN_MSG;
import static com.craxiom.networksurveyplus.messages.QcdmConstants.LOG_LTE_NAS_EMM_OTA_OUT_MSG;
import static com.craxiom.networksurveyplus.messages.QcdmConstants.LOG_LTE_NAS_ESM_OTA_IN_MSG;
import static com.craxiom.networksurveyplus.messages.QcdmConstants.LOG_LTE_NAS_ESM_OTA_OUT_MSG;
import static com.craxiom.networksurveyplus.messages.QcdmConstants.LTE_UL_CCCH;
import static com.craxiom.networksurveyplus.messages.QcdmConstants.LTE_UL_DCCH;

/**
 * Utility class for converting a QcdmMessage into different formats. For example: when logging to a file,
 * the QCDM message will need to be transformed into a pcap byte array, and when sending messages
 * to a connected server, the QCDM will need to be a protobuf-defined message, for example: an
 * {@link com.craxiom.messaging.LteRrc} record.
 *
 * @since 0.2.0
 */
public class QcdmMessageUtils
{
    private static final short PPI_GPS_FLAG_LAT = 2;
    private static final short PPI_GPS_FLAG_LON = 4;
    private static final short PPI_GPS_FLAG_ALT = 8;
    private static final String LTE_RRC_MESSAGE_TYPE = "LteRrc";
    private static final String LTE_NAS_MESSAGE_TYPE = "LteNas";

    /**
     * Given an {@link QcdmMessage} that contains an LTE RRC OTA message, convert it to a Network Survey Messaging
     * {@link LteRrc} protobuf object so that it can be sent over an MQTT connection.
     *
     * @param qcdmMessage  The QCDM message to convert into a pcap record.
     * @param location     The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                     location will be added to the PPI header.
     * @param deviceId     The Device ID to set as the "Device Serial Number" on the protobuf message.
     * @param missionId    The Mission ID to set on the protobuf message.
     * @param mqttClientId The MQTT client ID to set as the "Device Name" on the protobuf message. If null it won't be set.
     * @return The pcap record byte array to write to a pcap file.
     */
    public static LteRrc convertLteRrcOtaMessage(QcdmMessage qcdmMessage, Location location, String deviceId, String missionId, String mqttClientId)
    {
        Timber.v("Handling an LTE RRC message");

        final LteRrc.Builder lteRrcBuilder = LteRrc.newBuilder();
        final LteRrcData.Builder lteRrcDataBuilder = LteRrcData.newBuilder();

        lteRrcBuilder.setVersion(BuildConfig.MESSAGING_API_VERSION);
        lteRrcBuilder.setMessageType(LTE_RRC_MESSAGE_TYPE);

        lteRrcDataBuilder.setDeviceSerialNumber(deviceId);
        if (mqttClientId != null) lteRrcDataBuilder.setDeviceName(mqttClientId);
        lteRrcDataBuilder.setMissionId(missionId);
        lteRrcDataBuilder.setDeviceTime(getRfc3339String(ZonedDateTime.now()));
        lteRrcDataBuilder.setAltitude((float) location.getAltitude());
        lteRrcDataBuilder.setLatitude(location.getLatitude());
        lteRrcDataBuilder.setLongitude(location.getLongitude());

        final byte[] logPayload = qcdmMessage.getLogPayload();
        lteRrcDataBuilder.setRawMessage(ByteString.copyFrom(logPayload));

        // The base header is 6 bytes:
        // 1 byte each for Ext Header Version, RRC Rel, RRC Version, and Bearer ID
        // 2 bytes for Physical Cell ID
        final int extHeaderVersion = logPayload[0] & 0xFF;
        // Next is the extended header, which is either 7, 9, 11, or 13 bytes:
        // freq is 2 bytes if extHeaderVersion < 8 and 4 bytes otherwise
        // 2 bytes for System Frame Number (12 bits) & Subframe Number (4 bits)
        // 1 byte for Channel Type
        // Optional 4 bytes of padding if the length is != actual length. Something about the SIB mask is present.
        // 2 bytes for length
        final int frequencyLength = extHeaderVersion < 8 ? 2 : 4;
        int channelType = logPayload[6 + frequencyLength + 2];
        int gsmtapChannelType = getGsmtapRrcChannelType(extHeaderVersion, channelType);

        lteRrcDataBuilder.setChannelTypeValue(gsmtapChannelType + 1); // Here we offset by 1 to match with the LteRrcChannelType values

        lteRrcBuilder.setData(lteRrcDataBuilder.build());

        return lteRrcBuilder.build();
    }

    /**
     * Given a {@link QcdmMessage} that contains an LTE RRC OTA message, convert it to a pcap record byte array that
     * can be consumed by tools like Wireshark.
     *
     * @param qcdmMessage The QCDM message to convert into a pcap record.
     * @param location    The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                    location will be added to the PPI header.
     * @return The pcap record byte array to write to a pcap file.
     */
    public static byte[] convertLteRrcOtaMessage(QcdmMessage qcdmMessage, Location location)
    {
        Timber.v("Handling an LTE RRC message");

        final byte[] logPayload = qcdmMessage.getLogPayload();

        // The base header is 6 bytes:
        // 1 byte each for Ext Header Version, RRC Rel, RRC Version, and Bearer ID
        // 2 bytes for Physical Cell ID
        final int extHeaderVersion = logPayload[0] & 0xFF;
        final int pci = ParserUtils.getShort(logPayload, 4, ByteOrder.LITTLE_ENDIAN);

        // Next is the extended header, which is either 7, 9, 11, or 13 bytes:
        // freq is 2 bytes if extHeaderVersion < 8 and 4 bytes otherwise
        // 2 bytes for System Frame Number (12 bits) & Subframe Number (4 bits)
        // 1 byte for Channel Type
        // Optional 4 bytes of padding if the length is != actual length. Something about the SIB mask is present.
        // 2 bytes for length
        final int frequencyLength = extHeaderVersion < 8 ? 2 : 4;

        final int earfcn;
        if (frequencyLength == 2)
        {
            earfcn = ParserUtils.getShort(logPayload, 6, ByteOrder.LITTLE_ENDIAN);
        } else
        {
            earfcn = ParserUtils.getInteger(logPayload, 6, ByteOrder.LITTLE_ENDIAN);
        }

        // Mobile Sentinel seems to take the system frame number and combine it with the PCI into a 4 byte int value
        // The System Frame Number as the last 12 bits, and the PCI as the first 16 bits.
        // Shift the SFN right by 4 bytes because I *think* the last 4 bytes represent the subframe number (0 - 9).
        final short sfnAndsubfn = ParserUtils.getShort(logPayload, 6 + frequencyLength, ByteOrder.LITTLE_ENDIAN);
        final int subframeNumber = sfnAndsubfn & 0xF;
        final int sfn = sfnAndsubfn >>> 4;

        final int sfnAndPci = sfn | (pci << 16);

        // If the length field (last two bytes of the extended header) is != to the actual length then the extended header is 4 bytes longer
        int baseAndExtHeaderLength = 6 + frequencyLength + 5;
        int length = ParserUtils.getShort(logPayload, baseAndExtHeaderLength - 2, ByteOrder.LITTLE_ENDIAN);
        if (length != logPayload.length - baseAndExtHeaderLength)
        {
            baseAndExtHeaderLength += 4;
            length = ParserUtils.getShort(logPayload, baseAndExtHeaderLength - 2, ByteOrder.LITTLE_ENDIAN);
        }

        int channelType = logPayload[6 + frequencyLength + 2];

        boolean isUplink = channelType == LTE_UL_CCCH || channelType == LTE_UL_DCCH;

        final int gsmtapChannelType = getGsmtapRrcChannelType(extHeaderVersion, channelType);
        if (gsmtapChannelType == -1)
        {
            Timber.w("Unknown channel type received for LOG_LTE_RRC_OTA_MSG_LOG_C: %d", channelType);
            return null;
        }

        Timber.v("baseAndExtHeaderLength=%d, providedLength=%d", baseAndExtHeaderLength, length);

        final byte[] message = Arrays.copyOfRange(logPayload, baseAndExtHeaderLength, baseAndExtHeaderLength + length);
        final byte[] gsmtapHeader = getGsmtapHeader(GsmtapConstants.GSMTAP_TYPE_LTE_RRC, gsmtapChannelType, earfcn, isUplink, sfnAndPci, subframeNumber);
        final byte[] layer4Header = getLayer4Header(gsmtapHeader.length + message.length);
        final byte[] layer3Header = getLayer3Header(layer4Header.length + gsmtapHeader.length + message.length, qcdmMessage.getSimId());
        final byte[] ppiPacketHeader = getPpiPacketHeader(location);
        final long currentTimeMillis = System.currentTimeMillis();
        final byte[] pcapRecordHeader = getPcapRecordHeader(currentTimeMillis / 1000, (currentTimeMillis * 1000) % 1_000_000,
                ppiPacketHeader.length + layer3Header.length + layer4Header.length + gsmtapHeader.length + message.length);

        return concatenateByteArrays(pcapRecordHeader, ppiPacketHeader, layer3Header, layer4Header, gsmtapHeader, message);
    }

    /**
     * Given an {@link QcdmMessage} that contains an LTE NAS OTA message, convert it to a Network Survey Messaging
     * {@link LteNas} protobuf object so that it can be sent over an MQTT connection.
     * <p>
     * Information on how to parse LTE NAS messages was found in Mobile Sentinel:
     * https://github.com/RUB-SysSec/mobile_sentinel/blob/8485ef811cfbba7ab8b9d39bee7b38ae9072cce8/app/src/main/python/parsers/qualcomm/diagltelogparser.py#L1109
     *
     * @param qcdmMessage  The QCDM message to convert into a pcap record.
     * @param location     The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                     location will be added to the PPI header.
     * @param deviceId     The Device ID to set as the "Device Serial Number" on the protobuf message.
     * @param missionId    The Mission ID to set on the protobuf message.
     * @param mqttClientId The MQTT client ID to set as the "Device Name" on the protobuf message. If null it won't be set.
     * @return The pcap record byte array to write to a pcap file.
     */
    public static LteNas convertLteNasMessage(QcdmMessage qcdmMessage, Location location, String deviceId, String missionId, String mqttClientId)
    {
        Timber.v("Handling an LTE NAS message");

        final LteNasData.Builder lteNasDataBuilder = LteNasData.newBuilder();

        lteNasDataBuilder.setDeviceSerialNumber(deviceId);
        if (mqttClientId != null) lteNasDataBuilder.setDeviceName(mqttClientId);
        lteNasDataBuilder.setMissionId(missionId);
        lteNasDataBuilder.setDeviceTime(getRfc3339String(ZonedDateTime.now()));
        lteNasDataBuilder.setAltitude((float) location.getAltitude());
        lteNasDataBuilder.setLatitude(location.getLatitude());
        lteNasDataBuilder.setLongitude(location.getLongitude());
        lteNasDataBuilder.setRawMessage(ByteString.copyFrom(qcdmMessage.getLogPayload()));

        final int logType = qcdmMessage.getLogType();
        final boolean isPlain = logType == LOG_LTE_NAS_EMM_OTA_IN_MSG || logType == LOG_LTE_NAS_EMM_OTA_OUT_MSG
                || logType == LOG_LTE_NAS_ESM_OTA_IN_MSG || logType == LOG_LTE_NAS_ESM_OTA_OUT_MSG;
        final LteNasChannelType gsmtapChannelType = isPlain ? LteNasChannelType.PLAIN : LteNasChannelType.SEC_HEADER;

        lteNasDataBuilder.setChannelType(gsmtapChannelType);

        final LteNas.Builder lteNasBuilder = LteNas.newBuilder();
        lteNasBuilder.setVersion(BuildConfig.MESSAGING_API_VERSION);
        lteNasBuilder.setMessageType(LTE_NAS_MESSAGE_TYPE);
        lteNasBuilder.setData(lteNasDataBuilder.build());

        return lteNasBuilder.build();
    }

    /**
     * Given a {@link QcdmMessage} that contains an LTE NAS message, convert it to a pcap record byte array that
     * can be consumed by tools like Wireshark.
     *
     * @param qcdmMessage The QCDM message to convert into a pcap record.
     * @param location    The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                    location will be added to the PPI header.
     * @return The pcap record byte array to write to a pcap file.
     */
    public static byte[] convertLteNasMessage(QcdmMessage qcdmMessage, Location location)
    {
        Timber.v("Handling an LTE NAS message");

        final byte[] logPayload = qcdmMessage.getLogPayload();

        final byte[] signalingMessage = Arrays.copyOfRange(logPayload, 4, logPayload.length);
        final int logType = qcdmMessage.getLogType();
        final boolean isUplink = (logType & 0x01) == 1; // All uplink log types are odd
        final boolean isPlain = logType == LOG_LTE_NAS_EMM_OTA_IN_MSG || logType == LOG_LTE_NAS_EMM_OTA_OUT_MSG
                || logType == LOG_LTE_NAS_ESM_OTA_IN_MSG || logType == LOG_LTE_NAS_ESM_OTA_OUT_MSG;
        final int gsmtapChannelType = isPlain ? LteNasSubtypes.GSMTAP_LTE_NAS_PLAIN.ordinal() : LteNasSubtypes.GSMTAP_LTE_NAS_SEC_HEADER.ordinal();

        final byte[] gsmtapHeader = getGsmtapHeader(GsmtapConstants.GSMTAP_TYPE_LTE_NAS, gsmtapChannelType, 0, isUplink, 0, 0);
        final byte[] layer4Header = getLayer4Header(gsmtapHeader.length + signalingMessage.length);
        final byte[] layer3Header = getLayer3Header(layer4Header.length + gsmtapHeader.length + signalingMessage.length, qcdmMessage.getSimId());
        final byte[] ppiPacketHeader = getPpiPacketHeader(location);
        final long currentTimeMillis = System.currentTimeMillis();
        final byte[] pcapRecordHeader = getPcapRecordHeader(currentTimeMillis / 1000, (currentTimeMillis * 1000) % 1_000_000,
                ppiPacketHeader.length + layer3Header.length + layer4Header.length + gsmtapHeader.length + signalingMessage.length);

        return concatenateByteArrays(pcapRecordHeader, ppiPacketHeader, layer3Header, layer4Header, gsmtapHeader, signalingMessage);
    }

    /**
     * It seems that the channel type included in the QCDM header is a bit complicated. Based on all the different
     * versions of the header, the channel type maps differently to the GSMTAP subtype field. This method attempts to
     * map everything.
     * <p>
     * I could not find a specification document that outlined the mapping for each version number so I had to rely
     * solely on the Mobile Sentinel source code from the diagltelogparser.py file:
     * https://github.com/RUB-SysSec/mobile_sentinel/blob/8485ef811cfbba7ab8b9d39bee7b38ae9072cce8/app/src/main/python/parsers/qualcomm/diagltelogparser.py#L894
     *
     * @param versionNumber The version number at the start of the QCDM message header.
     * @param channelType   The channel type found in the QCDM header
     * @return The GSMTAP Channel Type / Subtype that specifies what kind of message the payload of the GSMTAP
     * frame contains.
     */
    @SuppressWarnings("SwitchStatementWithoutDefaultBranch")
    public static int getGsmtapRrcChannelType(int versionNumber, int channelType)
    {
        switch (versionNumber)
        {
            case 2: // 0x02
            case 3: // 0x03
            case 4: // 0x04
            case 6: // 0x06
            case 7: // 0x07
            case 8: // 0x08
            case 13: // 0x0d
            case 22: // 0x16
                switch (channelType)
                {
                    case 1:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message.ordinal(); // 4
                    case 2:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message.ordinal(); // 5
                    case 3:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_MCCH_Message.ordinal(); // 7
                    case 4:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message.ordinal(); // 6
                    case 5:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_CCCH_Message.ordinal(); // 0
                    case 6:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_DCCH_Message.ordinal(); // 1
                    case 7:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_CCCH_Message.ordinal(); // 2
                    case 8:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_DCCH_Message.ordinal(); // 3
                }

                break;

            case 9: // 0x09
            case 12: // 0x0c
                switch (channelType)
                {
                    case 8:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message.ordinal(); // 4
                    case 9:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message.ordinal(); // 5
                    case 10:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_MCCH_Message.ordinal(); // 7
                    case 11:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message.ordinal(); // 6
                    case 12:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_CCCH_Message.ordinal(); // 0
                    case 13:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_DCCH_Message.ordinal(); // 1
                    case 14:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_CCCH_Message.ordinal(); // 2
                    case 15:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_DCCH_Message.ordinal(); // 3
                }
                break;

            case 14: // 0x0e
            case 15: // 0x0f
            case 16: // 0x10
                switch (channelType)
                {
                    case 1:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message.ordinal(); // 4
                    case 2:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message.ordinal(); // 5
                    case 4:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_MCCH_Message.ordinal(); // 7
                    case 5:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message.ordinal(); // 6
                    case 6:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_CCCH_Message.ordinal(); // 0
                    case 7:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_DCCH_Message.ordinal(); // 1
                    case 8:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_CCCH_Message.ordinal(); // 2
                    case 9:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_DCCH_Message.ordinal(); // 3
                }
                break;

            case 19: // 0x13
            case 26: // 0x1a
                switch (channelType)
                {
                    case 1:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message.ordinal(); // 4
                    case 3:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message.ordinal(); // 5
                    case 6:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_MCCH_Message.ordinal(); // 7
                    case 7:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message.ordinal(); // 6
                    case 8:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_CCCH_Message.ordinal(); // 0
                    case 9:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_DCCH_Message.ordinal(); // 1
                    case 10:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_CCCH_Message.ordinal(); // 2
                    case 11:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_DCCH_Message.ordinal(); // 3
                    case 45:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message_NB.ordinal(); // 18
                    case 46:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message_NB.ordinal(); // 20
                    case 47:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message_NB.ordinal(); // 21
                    case 48:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_CCCH_Message_NB.ordinal(); // 14
                    case 49:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_DCCH_Message_NB.ordinal(); // 15
                    case 50:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_CCCH_Message_NB.ordinal(); // 16
                    case 52:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_DCCH_Message_NB.ordinal(); // 17
                }
                break;

            case 20: // 0x14
                switch (channelType)
                {
                    case 1:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message.ordinal(); // 4
                    case 2:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message.ordinal(); // 5
                    case 4:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_MCCH_Message.ordinal(); // 7
                    case 5:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message.ordinal(); // 6
                    case 6:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_CCCH_Message.ordinal(); // 0
                    case 7:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_DCCH_Message.ordinal(); // 1
                    case 8:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_CCCH_Message.ordinal(); // 2
                    case 9:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_DCCH_Message.ordinal(); // 3
                    case 54:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message_NB.ordinal(); // 18
                    case 55:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message_NB.ordinal(); // 20
                    case 56:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message_NB.ordinal(); // 21
                    case 57:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_CCCH_Message_NB.ordinal(); // 14
                    case 58:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_DCCH_Message_NB.ordinal(); // 15
                    case 59:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_CCCH_Message_NB.ordinal(); // 16
                    case 61:
                        return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_DCCH_Message_NB.ordinal(); // 17
                }
                break;
        }

        Timber.e("Could not map the provide version number (%d) and channel type (%d) to a GSM tap subtype", versionNumber, channelType);
        return -1;
    }

    /**
     * Concatenates the provided byte arrays to one long byte array.
     *
     * @param arrays The arrays to combine into one.
     * @return the new concatenated byte array.
     */
    private static byte[] concatenateByteArrays(byte[]... arrays)
    {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            for (byte[] byteArray : arrays)
            {
                outputStream.write(byteArray);
            }

            return outputStream.toByteArray();
        } catch (IOException e)
        {
            Timber.e(e, "A problem occured when trying to create the pcap record byte array");
        }

        return null;
    }

    /**
     * Constructs a byte array in the GSMTAP header format. The header is always the same except for the
     * payload type and subtype fields.
     * <p>
     * https://wiki.wireshark.org/GSMTAP
     * http://osmocom.org/projects/baseband/wiki/GSMTAP
     * <p>
     * From the Osmocom website:
     * <pre>
     *     struct gsmtap_hdr {
     *         uint8_t version;         version, set to 0x01 currently
     *         uint8_t hdr_len;         length in number of 32bit words
     *         uint8_t type;            see GSMTAP_TYPE_*
     *         uint8_t timeslot;        timeslot (0..7 on Um)
     *
     *         uint16_t arfcn;          ARFCN (frequency)
     *         int8_t signal_dbm;       signal level in dBm
     *         int8_t snr_db;           signal/noise ratio in dB
     *
     *         uint32_t frame_number;   GSM Frame Number (FN)
     *
     *         uint8_t sub_type;        Type of burst/channel, see above
     *         uint8_t antenna_nr;      Antenna Number
     *         uint8_t sub_slot;        sub-slot within timeslot
     *         uint8_t res;             reserved for future use (RFU)
     *
     *     } +attribute+((packed));
     * </pre>
     * <p>
     * I could not find much information for GSMTAP Version 3. The only place I see it is coming from Mobile Sentinel,
     * and the only difference I could see was that it add 12 bytes at the end of the GSMTAP header. The first 8 of
     * those bytes is for the device seconds, and the last 4 bytes is the device usec.
     *
     * @param payloadType       The type of payload that follows this GSMTAP header.
     * @param gsmtapChannelType The channel subtype.
     * @param arfcn             The ARFCN to include in the GSMTAP header (limited to 14 bits).
     * @param isUplink          True if the cellular payload represents an uplink message, false otherwise.
     * @param sfnAndPci         The System Frame Number as the last 12 bits, and the PCI as the first 16 bits.
     * @return The byte array for the GSMTAP header.
     */
    public static byte[] getGsmtapHeader(int payloadType, int gsmtapChannelType, int arfcn, boolean isUplink, int sfnAndPci, int subframeNumber)
    {
        // GSMTAP assumes the ARFCN fits in 14 bits, but the LTE spec has the EARFCN range go up to 65535
        if (arfcn < 0 || arfcn > 16_383) arfcn = 0;
        int arfcnAndUplink = isUplink ? arfcn | 0x4000 : arfcn;

        return new byte[]{
                (byte) 0x02, // GSMTAP version (2) (There is a version 3 but Wireshark does not seem to parse it)
                (byte) 0x04, // Header length in 32-bit words (4 words aka 16 bytes)
                (byte) (payloadType & 0xFF), // Payload type (1 byte)
                (byte) 0x00, // Time Slot
                (byte) ((arfcnAndUplink & 0xFF00) >>> 8), (byte) (arfcnAndUplink & 0x00FF), // PCS flag (bit 16), Uplink flag (bit 15), ARFCN (last 14 bits)
                (byte) 0x00, // Signal Level dBm
                (byte) 0x00, // Signal/Noise Ratio dB
                (byte) (sfnAndPci >>> 24), (byte) ((sfnAndPci & 0xFF0000) >>> 16), (byte) ((sfnAndPci & 0xFF00) >>> 8), (byte) (sfnAndPci & 0xFF), // GSM Frame Number
                (byte) (gsmtapChannelType & 0xFF), // Subtype - Type of burst/channel
                (byte) 0x00, // Antenna Number
                (byte) (subframeNumber & 0xFF), // Sub-Slot
                (byte) 0x00 // Reserved for future use
        };
    }

    /**
     * Constructs a byte array in the layer 4 header format (UDP header). The header is always the same except for the
     * total length field, which it calculates using the provided value (it adds 8 because the UDP header is 8 bytes).
     *
     * @param packetLength The length of the GSM TAP header, and the payload.
     * @return The byte array for the layer 4 header.
     */
    public static byte[] getLayer4Header(int packetLength)
    {
        final int totalLength = 8 + packetLength;

        return new byte[]{
                (byte) 0x12, (byte) 0x79, // Source Port (GSMTAP Port 4729)
                (byte) 0x12, (byte) 0x79, // Destination Port (GSMTAP Port 4729)
                (byte) ((totalLength & 0xFF00) >>> 8), (byte) (totalLength & 0x00FF), // Total length (layer 3 header plus all other headers and the payload, aka start of layer 3 header to end of packet)
                (byte) 0x00, (byte) 0x00, // checksum
        };
    }

    /**
     * Constructs a byte array in the layer 3 header format (IP header). The header is always the same except for the
     * total length field, which it calculates using the provided value (it adds 20 because the IP header is 20 bytes).
     *
     * @param packetLength The length of the layer 4 header, GSM TAP header, and the payload.
     * @param simId        The Subscription ID that will be used as the last octet of the destination IP address.
     * @return The byte array for the layer 3 header.
     */
    public static byte[] getLayer3Header(int packetLength, int simId)
    {
        final int totalLength = 20 + packetLength;

        return new byte[]{
                (byte) 0x45, // IPv4 version (4) and length (5 aka 20 bytes))
                (byte) 0x00, // Differentiated Services Codepoint
                (byte) ((totalLength & 0xFF00) >>> 8), (byte) (totalLength & 0x00FF), // Total length (layer 3 header plus all other headers and the payload, aka start of layer 3 header to end of packet)
                (byte) 0x00, (byte) 0x00, // Identification
                (byte) 0x00, (byte) 0x00, // Flags
                (byte) 0x40, // Time to live (64)
                (byte) 0x11, // Protocol (17 UDP)
                (byte) 0x00, (byte) 0x00, // Header checksum
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // Source IP
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) (simId & 0xFF), // Destination IP
        };
    }

    /**
     * Constructs a byte array in the CACE PPI packet header format as documented here:
     * https://media.blackhat.com/bh-us-11/Cache/BH_US_11_Cache_PPI-Geolocation_WP.pdf
     * The header specifies the version, length and data link type of the following packet. The length
     * field accounts only for PPI encapsulated data (i.e. does not include the Layer 3 size).
     *
     * @param location The current location to be used for adding latitude, longitude and altitude to the packet
     * @return The byte array for the PPI packet header
     */
    public static byte[] getPpiPacketHeader(Location location)
    {
        int ppiPacketHeaderSize = 8; // 1-byte version, 1-byte flags, 2-byte header length, 4-byte data link type (dlt)
        byte[] ppiFieldHeader = getPpiFieldHeader(location);
        int packetHeaderLength = ppiPacketHeaderSize + ppiFieldHeader.length;
        byte[] ppiPacketHeader = {
                (byte) 0x00, // version (0)
                (byte) 0x00, // flags (0)
                (byte) (packetHeaderLength & 0xFF), (byte) ((packetHeaderLength & 0xFF00) >>> 8),
                (byte) 0xe4, 0, 0, 0  // Link Layer Type (4 bytes): 228 is LINKTYPE_IPV4
        };
        return concatenateByteArrays(ppiPacketHeader, ppiFieldHeader);
    }

    /**
     * Following the PPI packet header, there are zero or more PPI field headers. There will be one
     * field header for each PPI tag. Possible tags are GPS, VECTOR, SENSOR or ANTENNA)
     *
     * @param location The current location to be used for adding latitude, longitude and altitude to the packet
     * @return The byte array for the PPI field header
     */
    private static byte[] getPpiFieldHeader(Location location)
    {
        if (location == null) return new byte[]{};

        byte[] geoTag = getGeoTag(location);
        byte[] fieldHeader = {
                (byte) 0x32, (byte) 0x75,  // PPI field header type GPS (30002)
                (byte) (geoTag.length & 0xFF), (byte) ((geoTag.length & 0xFF00) >>> 8) // GPS tag size
        };
        return concatenateByteArrays(fieldHeader, geoTag);
    }

    /**
     * Constructs a basic geo-tag header including the actual geo-fields (i.e. latitude, longitude, altitude).
     * The base header consists of:
     * 1-byte <i>version</i>; currently always set to 2
     * 1-byte <i>pad</i>; serves only to make the <i>len</i> field naturally aligned
     * 2-byte <i>len</i>; the length of the tag including the base header
     * 4-byte <i>present</i>; the bitmask indicating the fields present in the tag
     *
     * @param location The current location to be used for adding latitude, longitude and altitude to the header
     * @return The byte array for the geo-tag
     */
    private static byte[] getGeoTag(Location location)
    {
        byte[] geoTagHeader = {};

        if (location == null)
        {
            Timber.w("Current location could not be determined.");
            return geoTagHeader;
        } else
        {
            int geoTagSize = 8; // 1-byte version + 1-byte magic + 2-byte length + 4-byte fields bitmask
            geoTagSize += 8;
            int fieldsPresent = PPI_GPS_FLAG_LAT | PPI_GPS_FLAG_LON;

            long latitude = doubleToFixed37(location.getLatitude());
            long longitude = doubleToFixed37(location.getLongitude());

            if (location.hasAltitude())
            {
                geoTagSize += 4;
                fieldsPresent |= PPI_GPS_FLAG_ALT;
                long altitude = doubleToFixed64(location.getAltitude());

                geoTagHeader = new byte[]{
                        (byte) 0x02, // version
                        (byte) 0xCF, // PPI GPS magic
                        (byte) (geoTagSize & 0xFF), (byte) ((geoTagSize & 0xFF00) >>> 8),
                        (byte) (fieldsPresent & 0xFF), (byte) ((fieldsPresent & 0xFF00) >>> 8), (byte) ((fieldsPresent & 0xFF0000) >>> 16), (byte) (fieldsPresent >>> 24),
                        (byte) (latitude & 0xFF), (byte) ((latitude & 0xFF00) >>> 8), (byte) ((latitude & 0xFF0000) >>> 16), (byte) (latitude >>> 24),
                        (byte) (longitude & 0xFF), (byte) ((longitude & 0xFF00) >>> 8), (byte) ((longitude & 0xFF0000) >>> 16), (byte) (longitude >>> 24),
                        (byte) (altitude & 0xFF), (byte) ((altitude & 0xFF00) >>> 8), (byte) ((altitude & 0xFF0000) >>> 16), (byte) (altitude >>> 24)
                };
            } else
            {
                geoTagHeader = new byte[]{
                        (byte) 0x02, // version
                        (byte) 0xCF, // PPI GPS magic
                        (byte) (geoTagSize & 0xFF), (byte) ((geoTagSize & 0xFF00) >>> 8),
                        (byte) (fieldsPresent & 0xFF), (byte) ((fieldsPresent & 0xFF00) >>> 8), (byte) ((fieldsPresent & 0xFF0000) >>> 16), (byte) (fieldsPresent >>> 24),
                        (byte) (latitude & 0xFF), (byte) ((latitude & 0xFF00) >>> 8), (byte) ((latitude & 0xFF0000) >>> 16), (byte) (latitude >>> 24),
                        (byte) (longitude & 0xFF), (byte) ((longitude & 0xFF00) >>> 8), (byte) ((longitude & 0xFF0000) >>> 16), (byte) (longitude >>> 24),
                };
            }
        }
        return geoTagHeader;
    }

    /**
     * Returns the header that is placed at the beginning of each PCAP record.
     *
     * @param timeSec      The time that this pcap record was generated in seconds.
     * @param timeMicroSec The microseconds use to add more precision to the {@code timeSec} parameter.
     * @param length       The length of the pcap record (I think including this pcap record header).
     * @return The byte[] for the pcap record header.
     */
    public static byte[] getPcapRecordHeader(long timeSec, long timeMicroSec, int length)
    {
        return new byte[]{
                (byte) (timeSec & 0x00FF), (byte) ((timeSec & 0xFF00) >>> 8), (byte) ((timeSec & 0xFF0000) >>> 16), (byte) (timeSec >>> 24),
                (byte) (timeMicroSec & 0x00FF), (byte) ((timeMicroSec & 0xFF00) >>> 8), (byte) ((timeMicroSec & 0xFF0000) >>> 16), (byte) (timeMicroSec >>> 24),
                (byte) (length & 0xFF), (byte) ((length & 0xFF00) >>> 8), (byte) ((length & 0xFF0000) >>> 16), (byte) (length >>> 24), // Frame length
                (byte) (length & 0xFF), (byte) ((length & 0xFF00) >>> 8), (byte) ((length & 0xFF0000) >>> 16), (byte) (length >>> 24), // Capture length
        };
    }

    /**
     * Return an ISO 8601 combined date and time string for specified date/time.
     *
     * @param date The date object to use when generating the timestamp.
     * @return String with format {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME} (e.g. "2020-08-19T18:13:22.548+00:00")
     */
    private static String getRfc3339String(ZonedDateTime date)
    {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date);
    }
}
