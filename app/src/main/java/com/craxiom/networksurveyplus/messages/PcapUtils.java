package com.craxiom.networksurveyplus.messages;

import android.location.Location;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import timber.log.Timber;

import static com.craxiom.networksurveyplus.NetworkSurveyUtils.doubleToFixed37;
import static com.craxiom.networksurveyplus.NetworkSurveyUtils.doubleToFixed64;

/**
 * A collection of utilities to help with creating PCAP files and GSMTAP PCAP records.
 *
 * @since 0.2.0
 */
public class PcapUtils
{
    private static final short PPI_GPS_FLAG_LAT = 2;
    private static final short PPI_GPS_FLAG_LON = 4;
    private static final short PPI_GPS_FLAG_ALT = 8;

    private PcapUtils()
    {
    }

    /**
     * Constructs a byte array in the form of a PCAP record. The return value of this method can be added to a pcap file
     * to represent a GSMTAP PCAP record. Although the GSMTAP name has "GSM" in it, it is used to represent other
     * cellular protocols such as UMTS and LTE.
     *
     * @param payloadType       The type of payload that follows the GSMTAP header.
     * @param payload           The cellular payload that this PCAP record is for.
     * @param gsmtapChannelType The channel subtype.
     * @param arfcn             The ARFCN to include in the GSMTAP header (limited to 14 bits). For LTE this means not
     *                          all EARFCNs are supported, and if the EARFCN is greater than 16,383 it can't be passed here.
     * @param isUplink          True if the cellular payload represents an uplink message, false otherwise.
     * @param sfnAndPci         The System Frame Number as the last 12 bits, and the PCI as the first 16 bits.
     * @param subframeNumber    The cellular Subframe Number that the payload was sent over.
     * @param simId             The Subscription ID that will be used as the last octet of the destination IP address.
     * @param location          The current location to be used for adding latitude, longitude and altitude to the packet.
     * @return The byte array for the GSMTAP header.
     */
    public static byte[] getGsmtapPcapRecord(int payloadType, byte[] payload, int gsmtapChannelType, int arfcn,
                                             boolean isUplink, int sfnAndPci, int subframeNumber, int simId, Location location)
    {
        final byte[] gsmtapHeader = getGsmtapHeader(payloadType, gsmtapChannelType, arfcn, isUplink, sfnAndPci, subframeNumber);
        final byte[] layer4Header = getLayer4Header(gsmtapHeader.length + payload.length);
        final byte[] layer3Header = getLayer3Header(layer4Header.length + gsmtapHeader.length + payload.length, simId);
        final byte[] ppiPacketHeader = getPpiPacketHeader(location);
        final long currentTimeMillis = System.currentTimeMillis();
        final byte[] pcapRecordHeader = getPcapRecordHeader(currentTimeMillis / 1000, (currentTimeMillis * 1000) % 1_000_000,
                ppiPacketHeader.length + layer3Header.length + layer4Header.length + gsmtapHeader.length + payload.length);

        return concatenateByteArrays(pcapRecordHeader, ppiPacketHeader, layer3Header, layer4Header, gsmtapHeader, payload);
    }

    /**
     * Concatenates the provided byte arrays to one long byte array.
     *
     * @param arrays The arrays to combine into one.
     * @return the new concatenated byte array.
     */
    public static byte[] concatenateByteArrays(byte[]... arrays)
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
     * @param location The current location to be used for adding latitude, longitude and altitude to the packet.
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
}
