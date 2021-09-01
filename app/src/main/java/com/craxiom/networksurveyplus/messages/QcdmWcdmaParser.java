package com.craxiom.networksurveyplus.messages;

import android.location.Location;

import com.craxiom.messaging.LteNas;
import com.craxiom.messaging.WcdmaRrcChannelType;
import com.craxiom.messaging.WcdmaRrcData;
import com.craxiom.messaging.LteRrcData;
import com.craxiom.messaging.WcdmaRrc;
import com.craxiom.networksurveyplus.BuildConfig;
import com.google.protobuf.ByteString;

import java.nio.ByteOrder;
import java.time.ZonedDateTime;
import java.util.Arrays;

import timber.log.Timber;

import static com.craxiom.networksurveyplus.messages.QcdmConstants.WCDMA_SIGNALING_MESSAGES;

/**
 * Contains parser methods for converting the QCDM WCDMA messages to various formats, like pcap records or protobuf
 * objects.
 *
 * @since 0.2.0
 */
public class QcdmWcdmaParser
{
    private QcdmWcdmaParser()
    {
    }

    private static final String WCDMA_RRC_MESSAGE_TYPE = "WcdmaRrc";
    /**
     * Given a {@link QcdmMessage} that contains a WCDMA Signaling message {@link QcdmConstants#WCDMA_SIGNALING_MESSAGES},
     * convert it to a pcap record byte array that can be consumed by tools like Wireshark.
     * <p>
     * The base header structure for the WCDMA Signaling Message:
     * ******************************************
     * | Channel Type |  rbid  | Message Length |
     * |    1 byte    | 1 byte |     2 bytes    |
     * ******************************************
     * <p>
     * From there, the remainder varies depending on the channel type provided by QCDM.
     * <p>
     * Channel Type
     * *************************
     * | Base Header | Payload |
     * |   4 bytes   |    n    |
     * *************************
     * <p>
     * Channel Type Extended
     * **************************************
     * | Base Header | SIB Type | Payload |
     * |   4 bytes   |  1 byte  |    n    |
     * **************************************
     * <p>
     * Channel Type New
     * *********************************************
     * | Base Header | UARFCN  |   PSC   | Payload |
     * |   4 bytes   | 2 bytes | 2 bytes |    n    |
     * *********************************************
     * <p>
     * Channel Type New Extended
     * ********************************************************
     * | Base Header | UARFCN  |   PSC   | SIB Type | Payload |
     * |   4 bytes   | 2 bytes | 2 bytes |  1 byte  |    n    |
     * ********************************************************
     *
     * @param qcdmMessage The QCDM message to convert into a pcap record.
     * @param location    The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                    location will be added to the PPI header.
     * @return The pcap record byte array to write to a pcap file, or null if the message could not be parsed.
     */
    public static byte[] convertWcdmaSignalingMessage(QcdmMessage qcdmMessage, Location location)
    {
        Timber.v("Handling a WCDMA Signaling message");

        final byte[] logPayload = qcdmMessage.getLogPayload();

        final int channelType = logPayload[0] & 0xFF;
        //final int rbid = logPayload[1] & 0xFF;
        //final int messageLength = ParserUtils.getShort(logPayload, 2, ByteOrder.LITTLE_ENDIAN);

        int headerLength;
        int uarfcn = -1;
        int psc = -1;

        int subtype = getGsmtapWcdmaRrcChannelType(channelType);
        if (subtype != -1)
        {
            headerLength = 4;
        } else if ((subtype = getGsmtapWcdmaRrcChannelTypeExtended(channelType)) != -1)
        {
            headerLength = 5;

            final int sibType = logPayload[4] & 0xFF;
            subtype = getSubtypeFromSibType(sibType);
            if (subtype == -1)
            {
                Timber.e("Unknown WCDMA SIB Type %d", sibType);
                return null;
            }
        } else if ((subtype = getGsmtapWcdmaRrcChannelTypeNew(channelType)) != -1)
        {
            headerLength = 8;

            uarfcn = ParserUtils.getShort(logPayload, 4, ByteOrder.LITTLE_ENDIAN);
            psc = ParserUtils.getShort(logPayload, 6, ByteOrder.LITTLE_ENDIAN);
        } else if ((subtype = getGsmtapWcdmaRrcChannelTypeNewExtended(channelType)) != -1)
        {
            headerLength = 9;

            uarfcn = ParserUtils.getShort(logPayload, 4, ByteOrder.LITTLE_ENDIAN);
            psc = ParserUtils.getShort(logPayload, 6, ByteOrder.LITTLE_ENDIAN);

            final int sibType = logPayload[8] & 0xFF;
            subtype = getSubtypeFromSibTypeNew(sibType);
            if (subtype == -1)
            {
                Timber.e("Unknown WCDMA SIB Type %d", sibType);
                return null;
            }
        } else
        {
            Timber.e("Unknown WCDMA RRC Channel Type %d", channelType);
            return null;
        }

        final byte[] signalingMessage = Arrays.copyOfRange(logPayload, headerLength, logPayload.length);

        final boolean isUplink = subtype == UmtsRrcSubtypes.GSMTAP_RRC_SUB_UL_DCCH_Message.ordinal()
                || subtype == UmtsRrcSubtypes.GSMTAP_RRC_SUB_UL_CCCH_Message.ordinal()
                || subtype == UmtsRrcSubtypes.GSMTAP_RRC_SUB_UL_SHCCH_Message.ordinal();

        // TODO It is possible that the PSC can be passed where the LTE PCI was normally passed.
        return PcapUtils.getGsmtapPcapRecord(GsmtapConstants.GSMTAP_TYPE_UMTS_RRC, signalingMessage, subtype, uarfcn,
                isUplink, 0, 0, qcdmMessage.getSimId(), location);
    }

    /**
     * Given an {@link QcdmMessage} that contains an WCDMA RRC OTA message, convert it to a Network Survey Messaging
     * {@link WcdmaRrc} protobuf object so that it can be sent over an MQTT connection.
     * <p>
     * Information on how to parse WCDMA RRC messages was found in Mobile Sentinel:
     * https://github.com/RUB-SysSec/mobile_sentinel/blob/8485ef811cfbba7ab8b9d39bee7b38ae9072cce8/app/src/main/python/parsers/qualcomm/diagltelogparser.py#L1109
     *
     * @param qcdmMessage  The QCDM message to convert into a pcap record.
     * @param location     The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                     location will be added to the PPI header.
     * @param deviceId     The Device ID to set as the "Device Serial Number" on the protobuf message.
     * @param missionId    The Mission ID to set on the protobuf message.
     * @param mqttClientId The MQTT client ID to set as the "Device Name" on the protobuf message. If null it won't be set.
     * @return Mqtt message to be published and sent to the listening device.
     */

    public static WcdmaRrc convertWcdmaRrcOtaMessage(QcdmMessage qcdmMessage, Location location, String deviceId, String missionId, String mqttClientId)
    {
        Timber.e("Handling an WCDMA RRC message MQTT");

        final byte[] logPayload = qcdmMessage.getLogPayload();
        final int channelType = logPayload[0] & 0xFF;

        int headerLength;
        int uarfcn = -1;
        int psc = -1;

        int subtype = getGsmtapWcdmaRrcChannelType(channelType);
        if (subtype != -1)
        {
            headerLength = 4;
        } else if ((subtype = getGsmtapWcdmaRrcChannelTypeExtended(channelType)) != -1)
        {
            headerLength = 5;

            final int sibType = logPayload[4] & 0xFF;
            subtype = getSubtypeFromSibType(sibType);
            if (subtype == -1)
            {
                Timber.e("Unknown WCDMA SIB Type %d", sibType);
                return null;
            }
        } else if ((subtype = getGsmtapWcdmaRrcChannelTypeNew(channelType)) != -1)
        {
        } else if ((subtype = getGsmtapWcdmaRrcChannelTypeNewExtended(channelType)) != -1)
        {

            final int sibType = logPayload[8] & 0xFF;
            subtype = getSubtypeFromSibTypeNew(sibType);
            if (subtype == -1)
            {
                Timber.e("Unknown WCDMA SIB Type %d", sibType);
                return null;
            }
        } else
        {
            Timber.e("Unknown WCDMA RRC Channel Type %d", channelType);
            return null;
        }

        final WcdmaRrc.Builder wcdmaRrcBuilder = WcdmaRrc.newBuilder();
        final WcdmaRrcData.Builder wcdmaRrcDataBuilder = WcdmaRrcData.newBuilder();

        wcdmaRrcBuilder.setVersion(BuildConfig.MESSAGING_API_VERSION);
        wcdmaRrcBuilder.setMessageType(WCDMA_RRC_MESSAGE_TYPE);

        wcdmaRrcDataBuilder.setDeviceSerialNumber(deviceId);
        if (mqttClientId != null) wcdmaRrcDataBuilder.setDeviceName(mqttClientId);
        wcdmaRrcDataBuilder.setMissionId(missionId);
        wcdmaRrcDataBuilder.setDeviceTime(ParserUtils.getRfc3339String(ZonedDateTime.now()));
        wcdmaRrcDataBuilder.setAltitude((float) location.getAltitude());
        wcdmaRrcDataBuilder.setLatitude(location.getLatitude());
        wcdmaRrcDataBuilder.setLongitude(location.getLongitude());

        wcdmaRrcDataBuilder.setRawMessage(ByteString.copyFrom(logPayload));

        wcdmaRrcDataBuilder.setChannelTypeValue(subtype + 1); // Here we offset by 1 to match with the WcdmaRrcChannelType values
        wcdmaRrcBuilder.setData(wcdmaRrcDataBuilder.build());

        return wcdmaRrcBuilder.build();
    }

    /**
     * Specific for the original WCDMA Channel Type Map.
     * <p>
     * It seems that the channel type included in the QCDM header is a bit complicated. There are several QCDM Channel
     * Types for the same UMTS RRC Channel Subtype.
     * <p>
     * I could not find a specification document that outlined the mapping for each version number so I had to rely
     * solely on the Mobile Sentinel source code from the diagwcdmalogparser.py file:
     * https://github.com/RUB-SysSec/mobile_sentinel/blob/7fb1083e9f7ae233487db76afff11d5633338097/app/src/main/python/parsers/qualcomm/diagwcdmalogparser.py#L153
     * <p>
     * Which in looking further, it seems that QCSuper took their code from SCAT, and so did Mobile Sentinel:
     * https://github.com/fgsect/scat/blob/0e1d3a4/parsers/qualcomm/diagwcdmalogparser.py#L259
     *
     * @param channelType The channel type found in the QCDM header.
     * @return The GSMTAP Channel Type / Subtype that specifies what kind of message the payload of the GSMTAP frame
     * contains, or -1 if a mapping was not found.
     */
    public static int getGsmtapWcdmaRrcChannelType(int channelType)
    {
        switch (channelType)
        {
            // Channel Type Map
            case 0:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_UL_CCCH_Message.ordinal();
            case 1:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_UL_DCCH_Message.ordinal();
            case 2: // 0x02
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_DL_CCCH_Message.ordinal();
            case 3: // 0x03
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_DL_DCCH_Message.ordinal();
            case 4: // 0x04
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_BCCH_BCH_Message.ordinal(); // Encoded
            case 5: // 0x05
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_BCCH_FACH_Message.ordinal(); // Encoded
            case 6: // 0x06
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_PCCH_Message.ordinal();
            case 7: // 0x07
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_MCCH_Message.ordinal();
            case 8: // 0x08
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_MSCH_Message.ordinal();
            case 10: // 0x0A
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_System_Information_Container.ordinal();

            default:
                return -1;
        }
    }

    /**
     * Specific for the WCDMA Channel Type Map Extended Type.
     * <p>
     * It seems that the channel type included in the QCDM header is a bit complicated. There are several QCDM Channel
     * Types for the same UMTS RRC Channel Subtype.
     * <p>
     * I could not find a specification document that outlined the mapping for each version number so I had to rely
     * solely on the Mobile Sentinel source code from the diagwcdmalogparser.py file:
     * https://github.com/RUB-SysSec/mobile_sentinel/blob/7fb1083e9f7ae233487db76afff11d5633338097/app/src/main/python/parsers/qualcomm/diagwcdmalogparser.py#L153
     * <p>
     * Which in looking further, it seems that QCSuper took their code from SCAT, and so did Mobile Sentinel:
     * https://github.com/fgsect/scat/blob/0e1d3a4/parsers/qualcomm/diagwcdmalogparser.py#L259
     *
     * @param channelType The channel type found in the QCDM header.
     * @return The GSMTAP Channel Type / Subtype that specifies what kind of message the payload of the GSMTAP frame
     * contains, or -1 if a mapping was not found.
     */
    public static int getGsmtapWcdmaRrcChannelTypeExtended(int channelType)
    {
        switch (channelType)
        {
            // Channel Type Map Extended Type
            case 9:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_BCCH_BCH_Message.ordinal(); // Extension SIBs
            case 0xFE: // 254
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_BCCH_BCH_Message.ordinal(); // Decoded
            case 0xFF: // 255
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_BCCH_FACH_Message.ordinal(); // Decoded

            default:
                return -1;
        }
    }

    /**
     * Specific for the WCDMA Channel Type Map New.
     * <p>
     * It seems that the channel type included in the QCDM header is a bit complicated. There are several QCDM Channel
     * Types for the same UMTS RRC Channel Subtype.
     * <p>
     * I could not find a specification document that outlined the mapping for each version number so I had to rely
     * solely on the Mobile Sentinel source code from the diagwcdmalogparser.py file:
     * https://github.com/RUB-SysSec/mobile_sentinel/blob/7fb1083e9f7ae233487db76afff11d5633338097/app/src/main/python/parsers/qualcomm/diagwcdmalogparser.py#L153
     * <p>
     * Which in looking further, it seems that QCSuper took their code from SCAT, and so did Mobile Sentinel:
     * https://github.com/fgsect/scat/blob/0e1d3a4/parsers/qualcomm/diagwcdmalogparser.py#L259
     *
     * @param channelType The channel type found in the QCDM header.
     * @return The GSMTAP Channel Type / Subtype that specifies what kind of message the payload of the GSMTAP frame
     * contains, or -1 if a mapping was not found.
     */
    public static int getGsmtapWcdmaRrcChannelTypeNew(int channelType)
    {
        switch (channelType)
        {
            // Channel Type Map New
            case 0x80:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_UL_CCCH_Message.ordinal();
            case 0x81:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_UL_DCCH_Message.ordinal();
            case 0x82:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_DL_CCCH_Message.ordinal();
            case 0x83:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_DL_DCCH_Message.ordinal();
            case 0x84:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_BCCH_BCH_Message.ordinal(); // Encoded
            case 0x85:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_BCCH_FACH_Message.ordinal(); // Encoded
            case 0x86:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_PCCH_Message.ordinal();
            case 0x87:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_MCCH_Message.ordinal();
            case 0x88:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_MSCH_Message.ordinal();

            default:
                return -1;
        }
    }

    /**
     * Specific for the WCDMA Channel Type Map New Extended Type.
     * <p>
     * It seems that the channel type included in the QCDM header is a bit complicated. There are several QCDM Channel
     * Types for the same UMTS RRC Channel Subtype.
     * <p>
     * I could not find a specification document that outlined the mapping for each version number so I had to rely
     * solely on the Mobile Sentinel source code from the diagwcdmalogparser.py file:
     * https://github.com/RUB-SysSec/mobile_sentinel/blob/7fb1083e9f7ae233487db76afff11d5633338097/app/src/main/python/parsers/qualcomm/diagwcdmalogparser.py#L153
     * <p>
     * Which in looking further, it seems that QCSuper took their code from SCAT, and so did Mobile Sentinel:
     * https://github.com/fgsect/scat/blob/0e1d3a4/parsers/qualcomm/diagwcdmalogparser.py#L259
     *
     * @param channelType The channel type found in the QCDM header.
     * @return The GSMTAP Channel Type / Subtype that specifies what kind of message the payload of the GSMTAP frame
     * contains, or -1 if a mapping was not found.
     */
    public static int getGsmtapWcdmaRrcChannelTypeNewExtended(int channelType)
    {
        switch (channelType)
        {
            // Channel Type Map New Extended Type
            case 0x89:
            case 0xF0:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_BCCH_BCH_Message.ordinal();

            default:
                Timber.e("Could not map the provided WCDMA channel type (%d) to a GSM tap subtype", channelType);
                return -1;
        }
    }

    /**
     * Maps the WCDMA SIB Type to the GSMTAP Subtype. I could not find a specification reference for this so I had to
     * rely on SCAT (which QCSuper and Mobile Sentinel use as well).
     * <p>
     * https://github.com/fgsect/scat/blob/6d3a62ed29b3e51aa902003c31177fdb1ea2a0c2/parsers/qualcomm/diagwcdmalogparser.py#L278
     *
     * @param sibType The SIB Type to map to the GSMTAP Subtype
     * @return The GSMTAP Subtype, or -1 if a mapping could not be found.
     */
    public static int getSubtypeFromSibType(int sibType)
    {
        switch (sibType)
        {
            case 0:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_MasterInformationBlock.ordinal();
            case 1:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType1.ordinal();
            case 2:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType2.ordinal();
            case 3:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType3.ordinal();
            case 4:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType4.ordinal(); // Encoded
            case 5:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType5.ordinal(); // Encoded
            case 6:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType6.ordinal();
            case 7:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType7.ordinal();
            case 8:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType8.ordinal();
            case 9:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType9.ordinal();
            case 10:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType10.ordinal();
            case 11:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType11.ordinal();
            case 12:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType12.ordinal();
            case 13:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType13.ordinal();
            case 14:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType13_1.ordinal();
            case 15:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType13_2.ordinal();
            case 16:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType13_3.ordinal();
            case 17:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType13_4.ordinal();
            case 18:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType14.ordinal();
            case 19:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType15.ordinal();
            case 20:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType15_1.ordinal();
            case 21:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType15_2.ordinal();
            case 22:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType15_3.ordinal();
            case 23:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType16.ordinal();
            case 24:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType17.ordinal();
            case 25:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType15_4.ordinal();
            case 26:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType18.ordinal();
            case 27:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoTypeSB1.ordinal();
            case 28:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoTypeSB2.ordinal();
            case 29:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType15_5.ordinal();
            case 30:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType5bis.ordinal();
            case 31:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType11bis.ordinal();

            // Extension SIB
            case 66:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType11bis.ordinal();
            case 67:
                return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType19.ordinal();

            default:
                return -1;
        }
    }

    public static int getSubtypeFromSibTypeNew(int sibType)
    {
        if (sibType == 31)
        {
            return UmtsRrcSubtypes.GSMTAP_RRC_SUB_SysInfoType19.ordinal();
        } else
        {
            return getSubtypeFromSibType(sibType);
        }
    }
}
