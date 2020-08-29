package com.craxiom.networksurveyplus;

import android.os.Environment;

import com.craxiom.networksurveyplus.messages.DiagCommand;
import com.craxiom.networksurveyplus.messages.GsmtapConstants;
import com.craxiom.networksurveyplus.messages.LteRrcSubtypes;
import com.craxiom.networksurveyplus.messages.ParserUtils;
import com.craxiom.networksurveyplus.messages.QcdmMessage;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import timber.log.Timber;

import static com.craxiom.networksurveyplus.messages.QcdmConstants.*;

/**
 * Writes the QCDM messages to a PCAP file.
 * <p>
 * The PCAP file format can be found here:  https://wiki.wireshark.org/Development/LibpcapFileFormat#File_Format
 * <p>
 * This class handles writing the pcap file global header, and then it also adds the appropriate layer 3 (IP),
 * layer 4 (UDP), and GSM_TAP headers to the front of each QCDM message.
 * <p>
 * Credit goes to Wireshark, QCSuper (https://github.com/P1sec/QCSuper/blob/master/protocol/gsmtap.py), and
 * Mobile Sentinel (https://github.com/RUB-SysSec/mobile_sentinel/blob/master/app/src/main/python/writers/pcapwriter.py)
 * for the information on how to construct the appropriate headers to drop the QCDM messages in a pcap file.
 *
 * @since 0.1.0
 */
public class QcdmPcapWriter implements IQcdmMessageListener
{
    private static final String LOG_DIRECTORY_NAME = "NetworkSurveyPlusData";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
    private static final byte[] ETHERNET_HEADER = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00};

    /**
     * The 24 byte PCAP global header.
     */
    private static final byte[] PCAP_FILE_GLOBAL_HEADER = {(byte) 0xd4, (byte) 0xc3, (byte) 0xb2, (byte) 0xa1, // PCAP magic number in little endian
            2, 0, 4, 0, // Major and minor file version (2 bytes each)
            0, 0, 0, 0, // GMT Offset (4 bytes)
            0, 0, 0, 0, // Timestamp Accuracy: (4 bytes)
            (byte) 0xff, (byte) 0xff, 0, 0,  // Snapshot (length)
            (byte) 0xe4, 0, 0, 0  // Link Layer Type (4 bytes): 228 is LINKTYPE_IPV4
    };
    private final BufferedOutputStream outputStream;

    /**
     * Constructs a new QCDM pcap file writer.
     * <p>
     * This constructor creates the file, and writes out the PCAP global header.
     *
     * @throws IOException If an error occurs when creating or writing to the file.
     */
    QcdmPcapWriter() throws IOException
    {
        final File pcapFile = new File(createNewFilePath());
        pcapFile.getParentFile().mkdirs();

        outputStream = new BufferedOutputStream(new FileOutputStream(pcapFile));
        outputStream.write(PCAP_FILE_GLOBAL_HEADER);
        outputStream.flush();
    }

    @Override
    public void onQcdmMessage(QcdmMessage qcdmMessage)
    {
        try
        {
            if (qcdmMessage.getOpCode() == DiagCommand.DIAG_LOG_F)
            {
                Timber.d("Pcap Writer listener: %s", qcdmMessage);

                byte[] pcapRecord = null;

                final int logType = qcdmMessage.getLogType();
                switch (logType)
                {
                    case LOG_LTE_RRC_OTA_MSG_LOG_C:
                        pcapRecord = convertLteRrcOtaMessage(qcdmMessage);
                        break;
                }

                if (pcapRecord != null)
                {
                    Timber.v("Writing a message to the pcap file"); // TODO Delete me
                    outputStream.write(pcapRecord);
                    outputStream.flush();
                }
            }
        } catch (Exception e)
        {
            Timber.e(e, "Could not handle a QCDM message");
        }
    }

    /**
     * Closes the pcap file's output stream.
     */
    public void close()
    {
        try
        {
            outputStream.close();
        } catch (IOException e)
        {
            Timber.e(e, "Could not close the pcap file output stream");
        }
    }

    // TODO Javadoc
    private static byte[] convertLteRrcOtaMessage(QcdmMessage qcdmMessage)
    {
        Timber.v("Handling an LTE RRC message"); // TODO Delete me

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

        // TODO QCSuper seems to ignore if the channel type is != 254, 255, 9, or 10. Not sure why that is.
        int channelType = ParserUtils.getShort(logPayload, 6 + frequencyLength + 2, ByteOrder.LITTLE_ENDIAN);

        boolean isUplink = channelType == LTE_UL_CCCH || channelType == LTE_UL_DCCH;

        // TODO QCSuper subtracts 7 from the channelType if it is greater than LTE_UL_DCCH (8), but I have no idea why
        if (channelType > LTE_UL_DCCH) channelType -= 7;

        final int gsmtapChannelType = getGsmtapChannelType(channelType);
        if (gsmtapChannelType == -1)
        {
            Timber.w("Unknown channel type received for LOG_LTE_RRC_OTA_MSG_LOG_C: %d", channelType);
            return null;
        }

        Timber.d("baseAndExtHeaderLength=%d, providedLength=%d", baseAndExtHeaderLength, length);

        final byte[] message = Arrays.copyOfRange(logPayload, baseAndExtHeaderLength, baseAndExtHeaderLength + length);
        final byte[] gsmtapHeader = getGsmtapHeader(GsmtapConstants.GSMTAP_TYPE_LTE_RRC, gsmtapChannelType, earfcn, isUplink, sfnAndPci, subframeNumber);
        final byte[] layer4Header = getLayer4Header(gsmtapHeader.length + message.length);
        final byte[] layer3Header = getLayer3Header(layer4Header.length + gsmtapHeader.length + message.length);
        final long currentTimeMillis = System.currentTimeMillis();
        final byte[] pcapRecordHeader = getPcapRecordHeader(currentTimeMillis / 1000, (currentTimeMillis * 1000) % 1_000_000,
                layer3Header.length + layer4Header.length + gsmtapHeader.length + message.length);

        return concatenateByteArrays(pcapRecordHeader, layer3Header, layer4Header, gsmtapHeader, message);
    }

    // TODO Javadoc
    private static int getGsmtapChannelType(int channelType)
    {
        switch (channelType)
        {
            case LTE_BCCH_DL_SCH:
                return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message.ordinal();

            case LTE_PCCH:
                return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message.ordinal();

            case LTE_DL_CCCH:
                return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_CCCH_Message.ordinal();

            case LTE_DL_DCCH:
                return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_DL_DCCH_Message.ordinal();

            case LTE_UL_CCCH:
                return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_CCCH_Message.ordinal();

            case LTE_UL_DCCH:
                return LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_UL_DCCH_Message.ordinal();

            default:
                return -1;
        }
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
        int arfcnAndUplink = isUplink ? arfcn | 0x40 : arfcn;

        return new byte[]{
                (byte) 0x02, // GSMTAP version (2) (There is a version 3 but Wireshark does not seem to parse it
                (byte) 0x04, // Header length (4 aka 16 bytes)
                (byte) (payloadType & 0xFF), // Payload type (1 byte)
                (byte) 0x00, // Time Slot
                (byte) ((arfcnAndUplink & 0xFF00) >>> 8), (byte) (arfcnAndUplink & 0x00FF), // Uplink (bit 15) ARFCN (last 14 bits)
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
                (byte) 0x12, (byte) 0x79, // Source Port (GSMTAP Port 4729) // TODO Source port should be a client port
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
     * @return The byte array for the layer 3 header.
     */
    public static byte[] getLayer3Header(int packetLength)
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
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // Destination IP
        };
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
     * @return The full path to the public directory where we store the pcap files.
     */
    private String createNewFilePath()
    {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/" + LOG_DIRECTORY_NAME + "/" +
                Constants.PCAP_FILE_NAME_PREFIX + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + ".pcap";
    }
}
