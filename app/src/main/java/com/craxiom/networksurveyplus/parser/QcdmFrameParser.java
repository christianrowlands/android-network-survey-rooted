package com.craxiom.networksurveyplus.parser;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;
import com.craxiom.networksurveyplus.messages.QcdmMessage;
import com.craxiom.networksurveyplus.util.ParserUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * QCDM-specific frame parser that extracts QCDM frames from DiagRevealer messages.
 * Handles HDLC unescaping, CRC validation, and QCDM prefix detection.
 */
public class QcdmFrameParser implements FrameParser
{

    @Override
    public List<byte[]> extractFrames(DiagRevealerMessage diagRevealerMessage)
    {
        List<byte[]> frames = new ArrayList<>();

        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(diagRevealerMessage.payload))
        {
            while (inputStream.available() > 0)
            {
                final byte[] frameBytes = getNextDiagMessageBytes(inputStream);

                if (frameBytes != null && frameBytes.length > 0)
                {
                    frames.add(frameBytes);
                }
            }
        } catch (IOException e)
        {
            Timber.e(e, "Could not read the Diag Revealer message payload to extract frames");
        }

        return frames;
    }

    @Override
    public boolean validateFrame(byte[] frame)
    {
        if (frame == null || frame.length < 2)
        {
            return false;
        }

        try
        {
            final int frameLengthWithoutCrc = frame.length - 2;
            final short expectedCrc = ParserUtils.getShort(frame, frameLengthWithoutCrc, ByteOrder.LITTLE_ENDIAN);
            final short actualCrc = ParserUtils.calculateCrc16X25(frame, frameLengthWithoutCrc);

            if (actualCrc != expectedCrc)
            {
                Timber.w("Invalid CRC found on a diag message expected=%s, actual=%s",
                        Integer.toHexString(expectedCrc), Integer.toHexString(actualCrc));
                return false;
            }

            return true;
        } catch (Exception e)
        {
            Timber.e(e, "Error validating frame");
            return false;
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
    private byte[] getNextDiagMessageBytes(InputStream inputStream)
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
                            Timber.e("Found the 0x7d escape byte, but did not find 0x5e or 0x5d after it, instead found %s",
                                    Integer.toHexString(nextByte));
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
}
