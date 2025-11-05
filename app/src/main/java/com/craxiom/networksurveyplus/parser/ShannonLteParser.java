package com.craxiom.networksurveyplus.parser;

import android.location.Location;

import com.craxiom.networksurveyplus.messages.CraxiomConstants;
import com.craxiom.networksurveyplus.messages.GsmtapConstants;
import com.craxiom.networksurveyplus.messages.LteRrcSubtypes;
import com.craxiom.networksurveyplus.messages.PcapMessage;
import com.craxiom.networksurveyplus.util.ParserUtils;
import com.craxiom.networksurveyplus.util.PcapUtils;

import java.nio.ByteOrder;
import java.util.Arrays;

import timber.log.Timber;

/**
 * Parses Shannon LTE messages (RRC and NAS) and converts them to PCAP records.
 */
public class ShannonLteParser
{
    /**
     * Converts Shannon LTE RRC OTA message to PCAP record.
     *
     * @param payload  Shannon RRC payload (4-byte header + RRC message)
     * @param simId    SIM ID (0 or 1)
     * @param location GPS location
     * @return PcapMessage or null if parsing fails
     */
    public static PcapMessage convertLteRrcOtaMessage(byte[] payload, int simId, Location location)
    {
        if (payload == null || payload.length < 5)
        {
            Timber.w("Shannon RRC payload too short: %d bytes", payload != null ? payload.length : 0);
            return null;
        }

        try
        {
            // Parse Shannon RRC header (4 bytes)
            int channel = payload[0] & 0xFF;
            int direction = payload[1] & 0xFF;
            int length = ParserUtils.getShort(payload, 2, ByteOrder.LITTLE_ENDIAN) & 0xFFFF;

            // Validate length
            if (length + 4 != payload.length)
            {
                Timber.w("Shannon RRC length mismatch: header=%d, actual=%d", length, payload.length - 4);
                // Continue anyway - length field may be imprecise
            }

            // Extract RRC message
            byte[] rrcMessage = Arrays.copyOfRange(payload, 4, payload.length);

            // Map Shannon channel to GSMTAP subtype
            int gsmtapChannelType = getShannonLteRrcSubtype(channel, direction);
            if (gsmtapChannelType == -1)
            {
                Timber.w("Unknown Shannon RRC channel: %d, direction: %d", channel, direction);
                return null;
            }

            boolean isUplink = (direction == 1);

            // Create PCAP record
            // Note: EARFCN, PCI, SFN are 0 (metadata not available in 0x52 messages)
            // These will be added in Phase 4 by tracking 0x50 messages
            byte[] pcapRecord = PcapUtils.getGsmtapPcapRecord(
                    GsmtapConstants.GSMTAP_TYPE_LTE_RRC,  // type = 0x0D
                    rrcMessage,
                    gsmtapChannelType,
                    0,          // EARFCN (TODO: track from 0x50)
                    isUplink,
                    0,          // SFN and PCI (TODO: track from 0x50)
                    0,          // Subframe number (not in Shannon)
                    simId,
                    location);

            return new PcapMessage(pcapRecord, CraxiomConstants.LTE_RRC_MESSAGE_TYPE, gsmtapChannelType);
        } catch (Exception e)
        {
            Timber.e(e, "Error parsing Shannon LTE RRC message");
            return null;
        }
    }

    /**
     * Converts Shannon LTE NAS message to PCAP record.
     *
     * @param payload  Shannon NAS payload (4-byte header + NAS message)
     * @param isEmm    true for EMM (0x5A), false for ESM (0x5F)
     * @param simId    SIM ID (0 or 1)
     * @param location GPS location
     * @return PcapMessage or null if parsing fails
     */
    public static PcapMessage convertLteNasMessage(byte[] payload, boolean isEmm, int simId, Location location)
    {
        if (payload == null || payload.length < 5)
        {
            Timber.w("Shannon NAS payload too short: %d bytes", payload != null ? payload.length : 0);
            return null;
        }

        try
        {
            // Parse Shannon NAS header (4 bytes)
            int direction = payload[0] & 0xFF;
            int length = ParserUtils.getShort(payload, 1, ByteOrder.LITTLE_ENDIAN) & 0xFFFF;
            // byte 3 is spare

            // Validate length
            if (length != payload.length - 4)
            {
                Timber.w("Shannon NAS length mismatch: header=%d, actual=%d", length, payload.length - 4);
                return null;
            }

            // Extract NAS message
            byte[] nasMessage = Arrays.copyOfRange(payload, 4, payload.length);

            boolean isUplink = (direction == 1);

            // Shannon NAS messages are always plain (not security-wrapped)
            int gsmtapChannelType = 0;  // GSMTAP_LTE_NAS_PLAIN

            // Create PCAP record
            byte[] pcapRecord = PcapUtils.getGsmtapPcapRecord(
                    GsmtapConstants.GSMTAP_TYPE_LTE_NAS,  // type = 0x12
                    nasMessage,
                    gsmtapChannelType,
                    0,          // ARFCN (not used for NAS)
                    isUplink,
                    0,          // Frame number (not used for NAS)
                    0,          // Subframe (not used for NAS)
                    simId,
                    location);

            return new PcapMessage(pcapRecord, CraxiomConstants.LTE_NAS_MESSAGE_TYPE, gsmtapChannelType);
        } catch (Exception e)
        {
            Timber.e(e, "Error parsing Shannon LTE NAS message");
            return null;
        }
    }

    /**
     * Maps Shannon RRC channel and direction to GSMTAP subtype.
     *
     * @param channel   Shannon channel number (0-4)
     * @param direction Shannon direction (0=DL, 1=UL)
     * @return GSMTAP subtype or -1 if unknown
     */
    private static int getShannonLteRrcSubtype(int channel, int direction)
    {
        if (direction == 0)  // Downlink
        {
            switch (channel)
            {
                case 0:
                    return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_CCCH_Message.ordinal();  // 0
                case 1:
                    return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message.ordinal();  // 6
                case 2:
                    return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message.ordinal();  // 4
                case 3:
                    return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message.ordinal();  // 5
                case 4:
                    return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_DCCH_Message.ordinal();  // 1
                default:
                    return -1;
            }
        } else if (direction == 1)  // Uplink
        {
            switch (channel)
            {
                case 0:
                    return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_CCCH_Message.ordinal();  // 2
                case 4:
                    return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_DCCH_Message.ordinal();  // 3
                default:
                    return -1;
            }
        }
        return -1;
    }
}
