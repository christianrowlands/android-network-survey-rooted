package com.craxiom.networksurveyplus.parser;

import android.location.Location;

import com.craxiom.networksurveyplus.messages.CraxiomConstants;
import com.craxiom.networksurveyplus.messages.GsmtapConstants;
import com.craxiom.networksurveyplus.messages.LteNasSubtypes;
import com.craxiom.networksurveyplus.messages.LteRrcSubtypes;
import com.craxiom.networksurveyplus.messages.PcapMessage;
import com.craxiom.networksurveyplus.messages.QcdmMessage;
import com.craxiom.networksurveyplus.util.ParserUtils;
import com.craxiom.networksurveyplus.util.PcapUtils;

import java.nio.ByteOrder;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import timber.log.Timber;

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
public class QcdmLteParser
{
    /**
     * Given a {@link QcdmMessage} that contains an LTE RRC OTA message, convert it to a pcap record byte array that
     * can be consumed by tools like Wireshark.
     *
     * @param qcdmMessage The QCDM message to convert into a pcap record.
     * @param location    The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                    location will be added to the PPI header.
     * @return The pcap record to write to a pcap file or stream over MQTT.
     */
    public static PcapMessage convertLteRrcOtaMessage(QcdmMessage qcdmMessage, Location location)
    {
        Timber.v("Handling an LTE RRC message");

        final byte[] logPayload = qcdmMessage.getLogPayload();

        // The base header is 6 bytes:
        // 1 byte each for Ext Header Version, RRC Rel, RRC Version, and Bearer ID
        // 2 bytes for Physical Cell ID
        final int extHeaderVersion = logPayload[0] & 0xFF;
        final int pci = ParserUtils.getShort(logPayload, 4, ByteOrder.LITTLE_ENDIAN);

        Timber.v("LTE RRC Header Version: %d", extHeaderVersion);

        // Next is the extended header, which is either 7, 9, 11, or 13 bytes:
        // freq is 2 bytes if extHeaderVersion < 8 and 4 bytes otherwise
        // 2 bytes for System Frame Number (12 bits) & Subframe Number (4 bits)
        // 1 byte for Channel Type
        // Optional 4 bytes of padding if the length is != actual length. Something about the SIB mask is present.
        // 2 bytes for length
        final int frequencyLength = extHeaderVersion < 8 ? 2 : 4; // TODO Looking at mobile-insight-core log_packet.h#LteRrcOtaPacketFmt_v26, it seems that freq length might be 2 for version 26, but that might be a typo

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

        final int gsmtapChannelType = getGsmtapLteRrcSubtype(extHeaderVersion, channelType);
        if (gsmtapChannelType == -1)
        {
            Timber.w("Unknown channel type received for LOG_LTE_RRC_OTA_MSG_LOG_C: %d", channelType);
            return null;
        }

        Timber.v("baseAndExtHeaderLength=%d, providedLength=%d", baseAndExtHeaderLength, length);

        final byte[] message = Arrays.copyOfRange(logPayload, baseAndExtHeaderLength, baseAndExtHeaderLength + length);
        final byte[] pcapRecord = PcapUtils.getGsmtapPcapRecord(GsmtapConstants.GSMTAP_TYPE_LTE_RRC, message, gsmtapChannelType, earfcn,
                isUplink, sfnAndPci, subframeNumber, qcdmMessage.getSimId(), location);

        return new PcapMessage(pcapRecord, CraxiomConstants.LTE_RRC_MESSAGE_TYPE, gsmtapChannelType);
    }
    /**
     * Given a {@link QcdmMessage} that contains an LTE NAS message, convert it to a pcap record byte array that
     * can be consumed by tools like Wireshark.
     *
     * @param qcdmMessage The QCDM message to convert into a pcap record.
     * @param location    The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                    location will be added to the PPI header.
     * @return The pcap record to write to a pcap file or stream over MQTT.
     */
    public static PcapMessage convertLteMibMessage(QcdmMessage qcdmMessage, Location location)
    {
        Timber.v("Handling an LTE MIB message");

        final byte[] logPayload = qcdmMessage.getLogPayload();
        // version == 2:
        final byte pkgVersion = logPayload[0];
        final int pci = ParserUtils.getShort(logPayload, 1, ByteOrder.LITTLE_ENDIAN);
        final int earfcn = ParserUtils.getInteger(logPayload, 3, ByteOrder.LITTLE_ENDIAN);
        final int sfn = ParserUtils.getShort(logPayload, 7, ByteOrder.LITTLE_ENDIAN);
        final byte txAnt = logPayload[9];
        final byte bw = logPayload[10];
        //# 1.4, 3, 5, 10, 15, 20 MHz - 6, 15, 25, 50, 75, 100 PRBs
        //    prb_to_bitval = {6: 0, 15: 1, 25: 2, 50: 3, 75: 4, 100: 5}
        final byte[] mib_payload = new byte[]{0,0,0};
        byte bitVal ;
        switch (bw){
            case 15: bitVal=1; break;
            case 25: bitVal=2; break;
            case 50: bitVal=3; break;
            case 75: bitVal=4; break;
            case 100: bitVal=5; break;
            case 6:
            default: bitVal=0;
        }
        // https://techlteworld.com/mib-master-information-block-in-lte/
        int sfn4 = (int)(sfn/4);
        mib_payload[0] = (byte) (bitVal << 5);
        mib_payload[0] +=  (2 << 2);
        mib_payload[0] += (sfn4 & 0b11000000) >> 6;
        mib_payload[1] = (byte) ((sfn4 & 0b111111) << 2);
        //Timber.i("MIB pkgVer:%d, pci:%d, earfcn:%d, sfn:%d, txAnt:%d, bw:%d; mib[0]:%d, mib[1]:%d", pkgVersion, pci , earfcn, sfn, txAnt, bw, mib_payload[0], mib_payload[1]);

        final int logType = qcdmMessage.getLogType();
        final boolean isUplink = false; // Always down-link ?
        final int gsmtapChannelType =  LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message.ordinal();

        final byte[] pcapRecord = PcapUtils.getGsmtapPcapRecord(GsmtapConstants.GSMTAP_TYPE_LTE_RRC, mib_payload, gsmtapChannelType,
                earfcn, isUplink, sfn, 0, qcdmMessage.getSimId(), location);

        return new PcapMessage(pcapRecord, CraxiomConstants.LTE_MIB_MESSAGE_TYPE, gsmtapChannelType);
    }

    /**
     * Given a {@link QcdmMessage} that contains an LTE NAS message, convert it to a pcap record byte array that
     * can be consumed by tools like Wireshark.
     *
     * @param qcdmMessage The QCDM message to convert into a pcap record.
     * @param location    The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                    location will be added to the PPI header.
     * @return The pcap record to write to a pcap file or stream over MQTT.
     */
    public static PcapMessage convertLteNasMessage(QcdmMessage qcdmMessage, Location location)
    {
        Timber.v("Handling an LTE NAS message");

        final byte[] logPayload = qcdmMessage.getLogPayload();

        final byte[] signalingMessage = Arrays.copyOfRange(logPayload, 4, logPayload.length);
        final int logType = qcdmMessage.getLogType();
        final boolean isUplink = (logType & 0x01) == 1; // All uplink log types are odd
        final boolean isPlain = logType == LOG_LTE_NAS_EMM_OTA_IN_MSG || logType == LOG_LTE_NAS_EMM_OTA_OUT_MSG
                || logType == LOG_LTE_NAS_ESM_OTA_IN_MSG || logType == LOG_LTE_NAS_ESM_OTA_OUT_MSG;
        final int gsmtapChannelType = isPlain ? LteNasSubtypes.GSMTAP_LTE_NAS_PLAIN.ordinal() : LteNasSubtypes.GSMTAP_LTE_NAS_SEC_HEADER.ordinal();

        final byte[] pcapRecord = PcapUtils.getGsmtapPcapRecord(GsmtapConstants.GSMTAP_TYPE_LTE_NAS, signalingMessage, gsmtapChannelType,
                0, isUplink, 0, 0, qcdmMessage.getSimId(), location);

        return new PcapMessage(pcapRecord, CraxiomConstants.LTE_NAS_MESSAGE_TYPE, gsmtapChannelType);
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
    public static int getGsmtapLteRrcSubtype(int versionNumber, int channelType)
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
            case 24: // 0x18
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
     * Return an ISO 8601 combined date and time string for specified date/time.
     *
     * @param date The date object to use when generating the timestamp.
     * @return String with format {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME} (e.g. "2020-08-19T18:13:22.548+00:00")
     */

}
