package com.craxiom.networksurveyplus.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.Consumer;

import timber.log.Timber;

/**
 * Some helpful parsing and utility methods that this app uses.
 *
 * @since 0.1.0
 */
public class ParserUtils
{
    // 0001 0000 0010 0001  (0, 5, 12)
    public static final int CRC_16_CCITT_POLYNOMIAL = 0x1021;

    private static final short[] CTC_TABLE_16 =
            {
                    (short) 0x0000, (short) 0x1189, (short) 0x2312, (short) 0x329b, (short) 0x4624, (short) 0x57ad, (short) 0x6536, (short) 0x74bf,
                    (short) 0x8c48, (short) 0x9dc1, (short) 0xaf5a, (short) 0xbed3, (short) 0xca6c, (short) 0xdbe5, (short) 0xe97e, (short) 0xf8f7,
                    (short) 0x1081, (short) 0x0108, (short) 0x3393, (short) 0x221a, (short) 0x56a5, (short) 0x472c, (short) 0x75b7, (short) 0x643e,
                    (short) 0x9cc9, (short) 0x8d40, (short) 0xbfdb, (short) 0xae52, (short) 0xdaed, (short) 0xcb64, (short) 0xf9ff, (short) 0xe876,
                    (short) 0x2102, (short) 0x308b, (short) 0x0210, (short) 0x1399, (short) 0x6726, (short) 0x76af, (short) 0x4434, (short) 0x55bd,
                    (short) 0xad4a, (short) 0xbcc3, (short) 0x8e58, (short) 0x9fd1, (short) 0xeb6e, (short) 0xfae7, (short) 0xc87c, (short) 0xd9f5,
                    (short) 0x3183, (short) 0x200a, (short) 0x1291, (short) 0x0318, (short) 0x77a7, (short) 0x662e, (short) 0x54b5, (short) 0x453c,
                    (short) 0xbdcb, (short) 0xac42, (short) 0x9ed9, (short) 0x8f50, (short) 0xfbef, (short) 0xea66, (short) 0xd8fd, (short) 0xc974,
                    (short) 0x4204, (short) 0x538d, (short) 0x6116, (short) 0x709f, (short) 0x0420, (short) 0x15a9, (short) 0x2732, (short) 0x36bb,
                    (short) 0xce4c, (short) 0xdfc5, (short) 0xed5e, (short) 0xfcd7, (short) 0x8868, (short) 0x99e1, (short) 0xab7a, (short) 0xbaf3,
                    (short) 0x5285, (short) 0x430c, (short) 0x7197, (short) 0x601e, (short) 0x14a1, (short) 0x0528, (short) 0x37b3, (short) 0x263a,
                    (short) 0xdecd, (short) 0xcf44, (short) 0xfddf, (short) 0xec56, (short) 0x98e9, (short) 0x8960, (short) 0xbbfb, (short) 0xaa72,
                    (short) 0x6306, (short) 0x728f, (short) 0x4014, (short) 0x519d, (short) 0x2522, (short) 0x34ab, (short) 0x0630, (short) 0x17b9,
                    (short) 0xef4e, (short) 0xfec7, (short) 0xcc5c, (short) 0xddd5, (short) 0xa96a, (short) 0xb8e3, (short) 0x8a78, (short) 0x9bf1,
                    (short) 0x7387, (short) 0x620e, (short) 0x5095, (short) 0x411c, (short) 0x35a3, (short) 0x242a, (short) 0x16b1, (short) 0x0738,
                    (short) 0xffcf, (short) 0xee46, (short) 0xdcdd, (short) 0xcd54, (short) 0xb9eb, (short) 0xa862, (short) 0x9af9, (short) 0x8b70,
                    (short) 0x8408, (short) 0x9581, (short) 0xa71a, (short) 0xb693, (short) 0xc22c, (short) 0xd3a5, (short) 0xe13e, (short) 0xf0b7,
                    (short) 0x0840, (short) 0x19c9, (short) 0x2b52, (short) 0x3adb, (short) 0x4e64, (short) 0x5fed, (short) 0x6d76, (short) 0x7cff,
                    (short) 0x9489, (short) 0x8500, (short) 0xb79b, (short) 0xa612, (short) 0xd2ad, (short) 0xc324, (short) 0xf1bf, (short) 0xe036,
                    (short) 0x18c1, (short) 0x0948, (short) 0x3bd3, (short) 0x2a5a, (short) 0x5ee5, (short) 0x4f6c, (short) 0x7df7, (short) 0x6c7e,
                    (short) 0xa50a, (short) 0xb483, (short) 0x8618, (short) 0x9791, (short) 0xe32e, (short) 0xf2a7, (short) 0xc03c, (short) 0xd1b5,
                    (short) 0x2942, (short) 0x38cb, (short) 0x0a50, (short) 0x1bd9, (short) 0x6f66, (short) 0x7eef, (short) 0x4c74, (short) 0x5dfd,
                    (short) 0xb58b, (short) 0xa402, (short) 0x9699, (short) 0x8710, (short) 0xf3af, (short) 0xe226, (short) 0xd0bd, (short) 0xc134,
                    (short) 0x39c3, (short) 0x284a, (short) 0x1ad1, (short) 0x0b58, (short) 0x7fe7, (short) 0x6e6e, (short) 0x5cf5, (short) 0x4d7c,
                    (short) 0xc60c, (short) 0xd785, (short) 0xe51e, (short) 0xf497, (short) 0x8028, (short) 0x91a1, (short) 0xa33a, (short) 0xb2b3,
                    (short) 0x4a44, (short) 0x5bcd, (short) 0x6956, (short) 0x78df, (short) 0x0c60, (short) 0x1de9, (short) 0x2f72, (short) 0x3efb,
                    (short) 0xd68d, (short) 0xc704, (short) 0xf59f, (short) 0xe416, (short) 0x90a9, (short) 0x8120, (short) 0xb3bb, (short) 0xa232,
                    (short) 0x5ac5, (short) 0x4b4c, (short) 0x79d7, (short) 0x685e, (short) 0x1ce1, (short) 0x0d68, (short) 0x3ff3, (short) 0x2e7a,
                    (short) 0xe70e, (short) 0xf687, (short) 0xc41c, (short) 0xd595, (short) 0xa12a, (short) 0xb0a3, (short) 0x8238, (short) 0x93b1,
                    (short) 0x6b46, (short) 0x7acf, (short) 0x4854, (short) 0x59dd, (short) 0x2d62, (short) 0x3ceb, (short) 0x0e70, (short) 0x1ff9,
                    (short) 0xf78f, (short) 0xe606, (short) 0xd49d, (short) 0xc514, (short) 0xb1ab, (short) 0xa022, (short) 0x92b9, (short) 0x8330,
                    (short) 0x7bc7, (short) 0x6a4e, (short) 0x58d5, (short) 0x495c, (short) 0x3de3, (short) 0x2c6a, (short) 0x1ef1, (short) 0x0f78
            };

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
                (bytes[offset + 7] & 0xff);
        return byteOrder == ByteOrder.LITTLE_ENDIAN ? Long.reverseBytes(value) : value;
    }

    /**
     * Reads the provided input stream and grabs the next Diag Revealer message.
     * <p>
     * It is first assumed that the next Diag Revealer message will be at the beginning of the input stream. If the
     * Diag Revealer header can not be read, the input stream is advanced just past the next 0x7e byte, and the header
     * is attempted to be read again.
     * <p>
     * This process of advancing to the next 0x7e byte is added to make this method more resilient to error states. If
     * the Diag port and the Diag Revealer application are working correctly we will never need to advance to the next
     * 0x7e byte because the header will always be the next 4 bytes to read.
     *
     * @param inputStream The input stream that is reading from the FIFO pipe. It is highly recommended to use a buffered
     *                    input stream when reading from a file to prevent excessive disk read operations.
     * @return The next Diag Revealer message that is found in the input stream, or null if something goes wrong.
     * @throws IOException if an error occurs when trying to read from the provided input stream.
     */
    public static DiagRevealerMessage getNextDiagRevealerMessage(InputStream inputStream) throws IOException
    {
        final byte[] headerBytes = new byte[4];
        while ((inputStream.read(headerBytes)) != -1)
        {
            final DiagRevealerMessageHeader header = DiagRevealerMessageHeader.parseDiagRevealerMessageHeader(headerBytes);
            if (header == null || header.messageType < 1 || 3 < header.messageType || header.messageLength < 1)
            {
                Timber.e("Could not parse out the Diag Revealer header");
                advanceTo7e(inputStream);
                continue;
            }

            final byte[] messageBytes = new byte[header.messageLength];

            final int bytesRead = inputStream.read(messageBytes);
            if (bytesRead != messageBytes.length)
            {
                Timber.e("Could not get the correct number of bytes from the FIFO diag revealer queue; bytesRead=%d, expectedLength=%d", bytesRead, messageBytes.length);
                return null;
            }

            return DiagRevealerMessage.parseDiagRevealerMessage(messageBytes, header);
        }

        return null;
    }

    /**
     * Moves the provide input stream to just after the next 0x7e byte.
     *
     * @param inputStream The input stream to advance past the 0x7e byte.
     */
    public static void advanceTo7e(InputStream inputStream) throws IOException
    {
        int nextByte;
        while ((nextByte = inputStream.read()) != -1)
        {
            if (nextByte == QcdmMessage.QCDM_FOOTER)
            {
                Timber.i("Advanced to the next 0x7e byte");
                return;
            }
        }
    }

    /**
     * Given a Diag Revealer message, pull out all the QCDM messages from the payload.
     * <p>
     * There can be more than one QCDM message in each Diag Revealer message. Each QCDM message is delimited by the 0x7e
     * byte. We also need to unescape any 0x7e or 0x7d bytes. For more details on that see the method
     * {@link ParserUtils#getNextDiagMessageBytes(InputStream)}
     *
     * @param diagRevealerMessage The Diag Revealer Message that contains the QCDM message(s) as a payload.
     */
    public static void processDiagRevealerMessage(DiagRevealerMessage diagRevealerMessage, Consumer<QcdmMessage> messageConsumer)
    {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(diagRevealerMessage.payload))
        {
            while (inputStream.available() > 0)
            {
                final byte[] diagMessageBytes = getNextDiagMessageBytes(inputStream);

                if (diagMessageBytes == null)
                {
                    Timber.e("Could not get the diag message bytes from the Diag Revealer message");
                    continue;
                }

                boolean hasQcdmPrefix = false;
                int simId = 0; // Assume Subscription ID is 0 unless we see otherwise in the QCDM prefix
                // Check to see if we need to remove the QCDM prefix (checking the first 4 bytes, but it is 8 bytes long.
                if (ByteBuffer.wrap(QcdmMessage.QCDM_PREFIX).equals(ByteBuffer.wrap(diagMessageBytes, 0, 4)))
                {
                    hasQcdmPrefix = true;
                    simId = getInteger(diagMessageBytes, 4, ByteOrder.LITTLE_ENDIAN);
                }

                final int diagMessageLengthWithoutCrc = diagMessageBytes.length - 2;
                final short expectedCrc = getShort(diagMessageBytes, diagMessageLengthWithoutCrc, ByteOrder.LITTLE_ENDIAN);
                final short crc = calculateCrc16X25(diagMessageBytes, diagMessageLengthWithoutCrc);

                //Timber.v("Escaped QCDM Message=%s", convertBytesToHexString(diagMessageBytes, 0, diagMessageBytes.length));

                if (crc != expectedCrc)
                {
                    Timber.w("Invalid CRC found on a diag message expected=%s, actual=%s", Integer.toHexString(expectedCrc), Integer.toHexString(crc));
                } else
                {
                    final byte[] qcdmBytes = Arrays.copyOfRange(diagMessageBytes, hasQcdmPrefix ? 8 : 0, diagMessageLengthWithoutCrc);
                    messageConsumer.accept(new QcdmMessage(qcdmBytes, simId));
                }
            }
        } catch (IOException e)
        {
            Timber.e(e, "Could not read the Diag Revealer message payload to extract the QCDM message bytes");
        }
    }

    /**
     * Given an input stream that contains QCDM messages (aka diag messages), pull out each individual Diag
     * message and return it as a byte array.
     * <p>
     * Each diag message is delimited by the byte 0x7e.
     * <p>
     * However, because the byte 0x7e can be contained in the actual diag message, it has to be escaped in the message
     * payload. So, to unescape the 0x7e byte we have to look for instances of "0x7d 0x5e" and replace it with "0x7e".
     * <p>
     * In addition, since 0x7d represents an escape character, 0x7d is escaped as "0x7d 0x5d".
     * <p>
     * Another way to look at these escape sequences is to know that 0x7d is the escape byte, and then the value that
     * follows is the original byte XORed with 0x20. So 0x7e is escaped with 0x7d and the next byte is set to
     * (0x7e ^ 0x20 ... aka 0x5e). For 0x7d, it is escaped with 0x7d and the next byte is set to (0x7d ^ 0x20 ... aka 0x5d).
     *
     * @param inputStream The input stream that contains the diag messages. It is highly recommended to use a buffered
     *                    input stream when reading from a file to prevent excessive disk read operations.
     * @return The unescaped next diag message.
     */
    public static byte[] getNextDiagMessageBytes(InputStream inputStream)
    {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            int nextByte;
            while ((nextByte = inputStream.read()) != -1)
            {
                if (nextByte != QcdmMessage.QCDM_FOOTER)
                {
                    if (nextByte == (byte) 0x7d)
                    {
                        nextByte = inputStream.read();
                        if (nextByte == (byte) 0x5e)
                        {
                            // We found the escape sequence for the 0x7e byte, drop it into the output stream
                            outputStream.write(0x7e);
                        } else if (nextByte == (byte) 0x5d)
                        {
                            // We found the escape sequence for the 0x7d byte, drop it into the output stream
                            outputStream.write(0x7d);
                        } else
                        {
                            Timber.e("Found the 0x7d escape byte, but did not find 0x5e or 0x5d after it, instead found %s", Integer.toHexString(nextByte));
                        }
                    } else
                    {
                        outputStream.write(nextByte);
                    }

                    continue;
                }

                return outputStream.toByteArray();
            }
        } catch (IOException e)
        {
            Timber.e(e, "Caught an exception when trying to get the next Diag Message bytes");
        }

        return null;
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
     * Calculates the 16-bit CRC X25 for the provided byte array to the specified point in the array.
     *
     * @param bytes        The byte array to run the CRC algorithm on.
     * @param stopPosition The number of bytes in the provided byte array to use when calculating the CRC.
     * @return The calculated CRC.
     */
    public static short calculateCrc16X25(byte[] bytes, int stopPosition)
    {
        short fcs = (short) 0xffff; // initialize
        int i;
        for (i = 0; i < stopPosition; i++)
        {
            fcs = (short) (((fcs & 0xFFFF) >>> 8) ^ CTC_TABLE_16[(fcs ^ bytes[i]) & 0xff]);
        }
        return (short) ~fcs;
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
            stringBuilder.append(String.format("%02x ", bytes[i]));
        }
        return stringBuilder.toString();
    }
}
