package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.GsmtapConstants;
import com.craxiom.networksurveyplus.messages.LteRrcSubtypes;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for parsing QCDM messages and creating the various pcap headers.
 *
 * @since 0.1.0
 */
public class QcdmTest
{
    @Test
    public void testGsmtapHeaderCreation()
    {
        //final byte[] bytes = {(byte) 0x10, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0xc0, (byte) 0xb0, (byte) 0x8d, (byte) 0xa6, (byte) 0xa5, (byte) 0x05, (byte) 0x36, (byte) 0xe8, (byte) 0xee, (byte) 0x00, (byte) 0x14, (byte) 0x0e, (byte) 0x30, (byte) 0x00, (byte) 0xed, (byte) 0x01, (byte) 0x6b, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x69, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x40, (byte) 0x01, (byte) 0x6e, (byte) 0x5c, (byte) 0xf6, (byte) 0xd5, (byte) 0xf0, (byte) 0x00, (byte) 0x00};

        final byte[] expectedBytes = {(byte) 0x02, (byte) 0x04, (byte) 0x0d, (byte) 0x00, (byte) 0x03, (byte) 0x6b,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xed, (byte) 0x00, (byte) 0x16,
                (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x00};

        final int sfnAndPci = 0x01ed0016;

        final byte[] gsmtapHeader = QcdmPcapWriter.getGsmtapHeader(GsmtapConstants.GSMTAP_TYPE_LTE_RRC, 0, 875, false, sfnAndPci, 9);

        assertArrayEquals(expectedBytes, gsmtapHeader);
    }

    @Test
    public void testMobileSentinelGsmtapHeaderExample()
    {
        final byte[] expectedBytes = {(byte) 0x02, (byte) 0x04, (byte) 0x0d, (byte) 0x00, (byte) 0x03, (byte) 0x6b,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xed, (byte) 0x00, (byte) 0x95,
                (byte) 0x06, (byte) 0x00, (byte) 0x09, (byte) 0x00};

        final byte[] gsmtapHeader = QcdmPcapWriter.getGsmtapHeader(GsmtapConstants.GSMTAP_TYPE_LTE_RRC, LteRrcSubtypes.GSMTAP_LTE_RRC_SUB_PCCH_Message.ordinal(), 875, false, 32309397, 9);

        assertArrayEquals(expectedBytes, gsmtapHeader);
    }

    @Test
    public void testLayer4HeaderCreation()
    {
        final byte[] expectedBytes = {(byte) 0x12, (byte) 0x79, (byte) 0x12, (byte) 0x79, (byte) 0x00, (byte) 0x2d,
                (byte) 0x00, (byte) 0x00};

        final byte[] header = QcdmPcapWriter.getLayer4Header(37);

        assertArrayEquals(expectedBytes, header);
    }

    @Test
    public void testLayer3HeaderCreation()
    {
        final byte[] expectedBytes = {(byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x41, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x11, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00};

        final byte[] header = QcdmPcapWriter.getLayer3Header(45);

        assertArrayEquals(expectedBytes, header);
    }

    @Test
    public void testPcapRecordHeaderCreation()
    {
        final byte[] expectedBytes = {(byte) 0xec, (byte) 0xc0, (byte) 0x36, (byte) 0x5f, (byte) 0x51, (byte) 0xe8,
                (byte) 0x06, (byte) 0x00, (byte) 0x4f, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x4f, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        final byte[] header = QcdmPcapWriter.getPcapRecordHeader(1597423852, 452689, 79);

        assertArrayEquals(expectedBytes, header);
    }
}
