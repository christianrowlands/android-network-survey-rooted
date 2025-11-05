package com.craxiom.networksurveyplus.parser;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;

import java.util.List;

/**
 * Vendor-specific frame parser that extracts and validates modem frames.
 * Each modem vendor has its own frame format, delimiters, and validation logic.
 */
public interface FrameParser
{
    /**
     * Extracts vendor-specific frames from DiagRevealer message.
     * Returns list of raw frames ready for message processing.
     *
     * @param diagRevealerMessage The message received from the revealer daemon
     * @return List of extracted frames (may be empty if no valid frames found)
     */
    List<byte[]> extractFrames(DiagRevealerMessage diagRevealerMessage);

    /**
     * Validates frame integrity (CRC, length checks, etc.).
     * Returns true if frame is valid and can be processed.
     *
     * @param frame The frame to validate
     * @return True if frame is valid, false otherwise
     */
    boolean validateFrame(byte[] frame);
}
