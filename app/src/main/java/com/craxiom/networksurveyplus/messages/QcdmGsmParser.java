package com.craxiom.networksurveyplus.messages;

import android.location.Location;

import java.util.Arrays;

import timber.log.Timber;

/**
 * Contains parser methods for converting the QCDM GSM messages to various formats, like pcap records or protobuf
 * objects.
 *
 * @since 0.4.0
 */
public class QcdmGsmParser
{
    private static final int GSM_SIGNAL_HEADER_LENGTH = 3;

    private QcdmGsmParser()
    {
    }

    /**
     * Given a {@link QcdmMessage} that contains a WCDMA Signaling message {@link QcdmConstants#WCDMA_SIGNALING_MESSAGES},
     * convert it to a pcap record byte array that can be consumed by tools like Wireshark.
     * <p>
     * The base header structure for the GSM RR Signaling Message:
     * *************************************************************
     * | Channel Type | Message Type | Message Length | L3 Message |
     * |    1 byte    |    1 byte    |     1 byte     |     n      |
     * *************************************************************
     * <p>
     * The code for this method was taken from SCAT:
     * https://github.com/fgsect/scat/blob/0c1fe579376460ba5cd42d82a556fb88cf89da61/parsers/qualcomm/diaggsmlogparser.py#L191
     *
     * @param qcdmMessage The QCDM message to convert into a pcap record.
     * @param location    The location to tie to the QCDM message when writing it to a pcap file. If null then no
     *                    location will be added to the PPI header.
     * @return The pcap record byte array to write to a pcap file, or null if the message could not be parsed.
     */
    public static byte[] convertGsmSignalingMessage(QcdmMessage qcdmMessage, Location location)
    {
        Timber.v("Handling a GSM RR Signaling message");

        final byte[] logPayload = qcdmMessage.getLogPayload();

        final int channelTypeDir = logPayload[0] & 0xFF;
        //final int messageType = logPayload[1] & 0xFF;
        final int messageLength = logPayload[2] & 0xFF;

        if (logPayload.length < messageLength + GSM_SIGNAL_HEADER_LENGTH)
        {
            Timber.e("The qcdm log payload is shorter than the defined length for a GSM signal message");
            return null;
        }

        byte[] l3Message = Arrays.copyOfRange(logPayload, GSM_SIGNAL_HEADER_LENGTH, messageLength + GSM_SIGNAL_HEADER_LENGTH);

        // Not sure why we take the channelTypeDir and do this to get the chan, but SCAT and all the other apps do it
        int chan = channelTypeDir & 0x7F;

        final int subtype = getGsmtapGsmChannelType(chan);

        if (chan == 0 || chan == 4)
        {
            // SDCCH/8 expects LAPDm header
            if (messageLength > 63)
            {
                Timber.w("The GSM signal message length is longer than 63 bytes, actual length=%d", messageLength);
                return null;
            }

            // SACCH/8 expects SACCH L1/LAPDm header
            byte[] sacchL1 = chan == 4 ? new byte[]{0x00, 0x00} : new byte[]{};

            l3Message = PcapUtils.concatenateByteArrays(
                    sacchL1, // SAACH header only if it is SAACH/8 (0x88?)
                    new byte[]{0x01}, // LAPDM Address Field
                    new byte[]{0x03}, // LAPDM Control Field
                    new byte[]{(byte) ((messageLength << 2) | 0x01)}, // LAPDM Length
                    l3Message);
        }

        // Any channel type dir that has the 0x80 bit set is downlink, everything else is uplink
        final boolean isUplink = (channelTypeDir & 0x80) == 0x00;

        return PcapUtils.getGsmtapPcapRecord(GsmtapConstants.GSMTAP_TYPE_UM, l3Message, subtype, 0,
                isUplink, 0, 0, qcdmMessage.getSimId(), location);
    }

    /**
     * Converts the QCDM chan to the GSMTAP defined channel type that needs to be included in the GSMTAP pcap header.
     *
     * @param channelType The channel type found in the QCDM header.
     * @return The GSMTAP Channel Type / Subtype that specifies what kind of message the payload of the GSMTAP frame
     * contains, or 0 if a mapping was not found.
     */
    public static int getGsmtapGsmChannelType(int channelType)
    {
        switch (channelType)
        {
            // Channel Type Map
            case 0:
                return GsmSubtypes.GSMTAP_CHANNEL_SDCCH8.ordinal();
            case 1:
                return GsmSubtypes.GSMTAP_CHANNEL_BCCH.ordinal();
            case 3: // 0x03
                return GsmSubtypes.GSMTAP_CHANNEL_CCCH.ordinal();
            case 4: // 0x04
                return 0x88; // Not sure why 4 maps to 0x88, but that is what SCAT and others do

            default:
                return 0;
        }
    }
}
