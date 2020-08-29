package com.craxiom.networksurveyplus.messages;

import java.nio.ByteOrder;

/**
 * Some helpful parsing and utility methods that this app uses.
 *
 * @since 0.1.0
 */
public class ParserUtils
{
    // 0001 0000 0010 0001  (0, 5, 12)
    public static final int CRC_16_CCITT_POLYNOMIAL = 0x1021;

    /**
     * Parse the provided bytes to extract a short value using the specified byte order.
     *
     * @param bytes     Bytes to parse
     * @param offset    Offset within bytes at which to start processing
     * @param byteOrder Byte order to apply
     * @return extracted value
     */
    public static short getShort(byte[] bytes, int offset, ByteOrder byteOrder)
    {
        final short value = (short) ((bytes[offset] & 0xFF) << 8 | (bytes[offset + 1] & 0xFF));
        return byteOrder == ByteOrder.LITTLE_ENDIAN ? reverseBytes(value) : value;
    }

    /**
     * Parse the provided bytes to extract an int value using the specified byte order.
     *
     * @param bytes     Bytes to parse
     * @param offset    Offset within bytes at which to start processing
     * @param byteOrder Byte order to apply
     * @return extracted value
     */
    public static int getInteger(byte[] bytes, int offset, ByteOrder byteOrder)
    {
        final int value = ((bytes[offset] & 0xff) << 24) |
                ((bytes[offset + 1] & 0xff) << 16) |
                ((bytes[offset + 2] & 0xff) << 8) |
                (bytes[offset + 3] & 0xff);
        return byteOrder == ByteOrder.LITTLE_ENDIAN ? Integer.reverseBytes(value) : value;
    }

    /**
     * Parse the provided bytes to extract a long value using the specified byte order.
     *
     * @param bytes     Bytes to parse
     * @param offset    Offset within bytes at which to start processing
     * @param byteOrder Byte order to apply
     * @return extracted value
     */
    public static long getLong(byte[] bytes, int offset, ByteOrder byteOrder)
    {
        final long value = ((long) (bytes[offset] & 0xff) << 56) |
                ((long) (bytes[offset + 1] & 0xff) << 48) |
                ((long) (bytes[offset + 2] & 0xff) << 40) |
                ((long) (bytes[offset + 3] & 0xff) << 32) |
                ((long) (bytes[offset + 4] & 0xff) << 24) |
                ((long) (bytes[offset + 5] & 0xff) << 16) |
                ((long) (bytes[offset + 6] & 0xff) << 8) |
                ((long) (bytes[offset + 7] & 0xff));
        return byteOrder == ByteOrder.LITTLE_ENDIAN ? Long.reverseBytes(value) : value;
    }

    /**
     * Calculates the CRC CCITT for the provided byte array.
     *
     * @param bytes The bytes to run through the CRC CCITT algorithm.
     * @return The 16 bit CRC stored in an int.
     */
    public static int calculateCrc16Ccitt(byte[] bytes)
    {
        int crc = 0xFFFF;

        for (byte b : bytes)
        {
            for (int i = 0; i < 8; i++)
            {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= CRC_16_CCITT_POLYNOMIAL;
            }
        }

        crc &= 0xffff;

        return crc;
    }

    /**
     * Converts a byte array to a hex string.
     *
     * @param bytes  The byte array to convert.
     * @param offset Offset within bytes to start processing.
     * @param length Number of bytes to process.
     * @return The byte array represented as a hex string.
     */
    public static String convertBytesToHexString(byte[] bytes, int offset, int length)
    {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.setLength(0);
        for (int i = offset, end = offset + length; i < end; i++)
        {
            stringBuilder.append(String.format("0x%02x ", bytes[i]));
        }
        return stringBuilder.toString();
    }

    /**
     * A custom implementation of {@link Short#reverseBytes(short)} because the JDK version uses the
     * regular right shift operator `>>` and we need to use the unsigned right shift operator `>>>`.
     *
     * @param s the value whose bytes are to be reversed
     * @return the value obtained by reversing (or, equivalently, swapping) the bytes in the specified {@code short} value.
     */
    public static short reverseBytes(short s)
    {
        return (short) (((s & 0xFF00) >>> 8) | (s << 8));
    }
}
