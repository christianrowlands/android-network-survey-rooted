package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.ParserUtils;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Unit tests for the parser utils class.
 *
 * @since 0.1.0
 */
public class ParserUtilsTest
{
    @Test
    public void testCrcCalculation()
    {
        final int expectedCrc = 0x29b1;
        final byte[] inputBytes = "123456789".getBytes(StandardCharsets.US_ASCII);

        final int crc = ParserUtils.calculateCrc16Ccitt(inputBytes);
        assertEquals(expectedCrc, crc);
    }

    @Test
    public void testCrcOtherCalculation()
    {
        final int expectedCrc = (short) 0x906e;
        final byte[] inputBytes = "123456789".getBytes(StandardCharsets.US_ASCII);

        final int crc = ParserUtils.calculateCrc16X25(inputBytes, inputBytes.length);
        assertEquals(expectedCrc, crc);
    }

    @Test
    public void testCrcQcdmMessage()
    {
        final short expectedCrc = 0x2d53;
        final byte[] qcdmBytes = {(byte) 0x60, (byte) 0x4c, (byte) 0x00, (byte) 0x32,
                (byte) 0x6b, (byte) 0xe5, (byte) 0xb9, (byte) 0xa4, (byte) 0xfb, (byte) 0x75, (byte) 0xdd, (byte) 0xee, (byte) 0x00, (byte) 0x13, (byte) 0x06, (byte) 0x6d, (byte) 0x73, (byte) 0x6d, (byte) 0x2f, (byte) 0x6d,
                (byte) 0x6f, (byte) 0x64, (byte) 0x65, (byte) 0x6d, (byte) 0x2f, (byte) 0x72, (byte) 0x6f, (byte) 0x6f, (byte) 0x74, (byte) 0x5f, (byte) 0x70, (byte) 0x64, (byte) 0x00, (byte) 0x31, (byte) 0xeb, (byte) 0xb9,
                (byte) 0xa4, (byte) 0x11, (byte) 0x06, (byte) 0x70, (byte) 0xe0, (byte) 0x8c, (byte) 0x67, (byte) 0x85, (byte) 0x25, (byte) 0x4d, (byte) 0x93, (byte) 0x8e, (byte) 0xf9, (byte) 0x11, (byte) 0xc4, (byte) 0xf1,
                (byte) 0x98, (byte) 0xdc, (byte) 0x4c, (byte) 0x32, (byte) 0xeb, (byte) 0xb9, (byte) 0xa4, (byte) 0x13, (byte) 0x07, (byte) 0x6d, (byte) 0x73, (byte) 0x6d, (byte) 0x2f, (byte) 0x6d, (byte) 0x6f, (byte) 0x64,
                (byte) 0x65, (byte) 0x6d, (byte) 0x2f, (byte) 0x77, (byte) 0x6c, (byte) 0x61, (byte) 0x6e, (byte) 0x5f, (byte) 0x70, (byte) 0x64, (byte) 0x00};

        final short crc = ParserUtils.calculateCrc16X25(qcdmBytes, qcdmBytes.length);
        assertEquals(expectedCrc, crc);
    }
}
