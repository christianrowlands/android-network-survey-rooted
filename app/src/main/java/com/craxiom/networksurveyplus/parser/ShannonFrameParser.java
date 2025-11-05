package com.craxiom.networksurveyplus.parser;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;
import com.craxiom.networksurveyplus.util.ParserUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Parses Shannon SDM (Samsung Diagnostic Monitor) frames.
 * Shannon frames: 0x7F [14-byte header] [payload] 0x7E
 * NO CRC, NO HDLC escaping.
 */
public class ShannonFrameParser implements FrameParser
{

    private static final byte SHANNON_START_MARKER = 0x7F;
    private static final byte SHANNON_END_MARKER = 0x7E;
    private static final int MIN_FRAME_SIZE = 16; // 0x7F + 14 bytes header + 0x7E

    @Override
    public List<byte[]> extractFrames(DiagRevealerMessage diagRevealerMessage)
    {
        List<byte[]> frames = new ArrayList<>();

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(diagRevealerMessage.payload))
        {
            while (inputStream.available() > 0)
            {
                byte[] frame = getNextShannonFrame(inputStream);
                if (frame != null)
                {
                    frames.add(frame);
                } else
                {
                    Timber.w("Failed to extract Shannon frame, skipping");
                }
            }
        } catch (IOException e)
        {
            Timber.e(e, "Error extracting Shannon frames");
        }

        return frames;
    }

    @Override
    public boolean validateFrame(byte[] frame)
    {
        if (frame == null || frame.length < MIN_FRAME_SIZE)
        {
            Timber.v("Shannon frame too short: %d bytes", frame != null ? frame.length : 0);
            return false;
        }

        // Check start marker
        if (frame[0] != SHANNON_START_MARKER)
        {
            Timber.v("Invalid Shannon start marker: 0x%02x", frame[0]);
            return false;
        }

        // Check end marker
        if (frame[frame.length - 1] != SHANNON_END_MARKER)
        {
            Timber.v("Invalid Shannon end marker: 0x%02x", frame[frame.length - 1]);
            return false;
        }

        // Parse lengths (little-endian)
        int length1 = ParserUtils.getShort(frame, 1, ByteOrder.LITTLE_ENDIAN) & 0xFFFF;
        int length2 = ParserUtils.getShort(frame, 4, ByteOrder.LITTLE_ENDIAN) & 0xFFFF;

        // Validate frame size matches length1
        if (frame.length != length1 + 2)
        {
            Timber.v("Shannon frame size mismatch: expected %d, got %d", length1 + 2, frame.length);
            return false;
        }

        // Validate inner/outer length consistency
        if (length2 + 3 != length1)
        {
            Timber.v("Shannon length mismatch: length2=%d, length1=%d (expected length2+3=length1)",
                    length2, length1);
            return false;
        }

        // Validate direction field
        byte direction = frame[8];
        if (direction != (byte) 0xA0 && direction != (byte) 0xA1 && direction != (byte) 0xA2)
        {
            Timber.v("Invalid Shannon direction: 0x%02x", direction);
            return false;
        }

        return true;
    }

    /**
     * Extracts next Shannon frame from input stream.
     * Shannon frames: 0x7F [length1 bytes] 0x7E
     * NO HDLC escaping, read raw bytes.
     */
    private byte[] getNextShannonFrame(ByteArrayInputStream inputStream)
    {
        // Find start marker
        int b;
        while ((b = inputStream.read()) != -1)
        {
            if (b == (SHANNON_START_MARKER & 0xFF))
            {
                // Read header to get length
                byte[] header = new byte[14];
                header[0] = (byte) b; // Include start marker

                int bytesRead = inputStream.read(header, 1, 13);
                if (bytesRead != 13)
                {
                    Timber.w("Incomplete Shannon header");
                    return null;
                }

                // Parse length1 (bytes 1-2, little-endian)
                int length1 = (header[1] & 0xFF) | ((header[2] & 0xFF) << 8);

                if (length1 < 11 || length1 > 8192)
                {
                    Timber.w("Invalid Shannon length1: %d", length1);
                    continue;
                }

                // Read remaining payload + end marker
                int remainingBytes = length1 - 13 + 1; // -13 (already read header minus start) +1 (end marker)
                byte[] frame = new byte[length1 + 2]; // +2 for start and end markers
                System.arraycopy(header, 0, frame, 0, 14);

                bytesRead = inputStream.read(frame, 14, remainingBytes);
                if (bytesRead != remainingBytes)
                {
                    Timber.w("Incomplete Shannon frame");
                    return null;
                }

                return frame;
            }
        }

        return null;
    }
}
