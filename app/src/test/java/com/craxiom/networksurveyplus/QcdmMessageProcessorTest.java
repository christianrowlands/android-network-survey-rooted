package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;
import com.craxiom.networksurveyplus.parser.QcdmFrameParser;
import com.craxiom.networksurveyplus.util.ParserUtils;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the frame based processing path ({@link QcdmFrameParser} and {@link QcdmMessageProcessor#onFrame(byte[])})
 * that the {@link FifoReadRunnable} uses.
 *
 * @since 0.6.0
 */
public class QcdmMessageProcessorTest
{
    /**
     * A diag revealer message containing a single LTE RRC OTA message.
     */
    private static final byte[] LTE_RRC_DIAG_REVEALER_MESSAGE = {(byte) 0x01, (byte) 0x00, (byte) 0x3f, (byte) 0x00, (byte) 0x41, (byte) 0xD7, (byte) 0xD6, (byte) 0x96, (byte) 0x34, (byte) 0xCC, (byte) 0xC5, (byte) 0xC1, (byte) 0x98, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0xc0, (byte) 0xb0, (byte) 0x21, (byte) 0x9e, (byte) 0xe8, (byte) 0x63, (byte) 0xf5, (byte) 0x1f, (byte) 0xef, (byte) 0x00, (byte) 0x14, (byte) 0x0e, (byte) 0x30, (byte) 0x00, (byte) 0xed, (byte) 0x01, (byte) 0x6b, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x59, (byte) 0x39, (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x40, (byte) 0x01, (byte) 0x7f, (byte) 0xbe, (byte) 0xeb, (byte) 0x01, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x50, (byte) 0x74, (byte) 0x7e};

    @Test
    public void testFrameParserExtractsValidFrame() throws IOException
    {
        final List<byte[]> frames = extractFrames();

        assertEquals(1, frames.size());
        assertTrue("The CRC of the extracted frame should be valid", new QcdmFrameParser().validateFrame(frames.get(0)));
    }

    @Test
    public void testOnFrameNotifiesListeners() throws IOException
    {
        final QcdmMessageProcessor processor = new QcdmMessageProcessor(new GpsListener());
        final AtomicInteger pcapMessageCount = new AtomicInteger(0);
        final AtomicInteger statusMessageCount = new AtomicInteger(0);
        processor.registerMessageListener(pcapMessage -> {
            assertNotNull(pcapMessage.getPcapRecord());
            pcapMessageCount.incrementAndGet();
        });
        processor.registerRecordsLoggedListener(statusMessage -> statusMessageCount.incrementAndGet());

        for (byte[] frame : extractFrames()) processor.onFrame(frame);

        assertEquals(1, pcapMessageCount.get());
        assertEquals(1, statusMessageCount.get());
    }

    @Test
    public void testOnFrameSkipsProcessingWithoutMessageListeners() throws IOException
    {
        final QcdmMessageProcessor processor = new QcdmMessageProcessor(new GpsListener());
        final AtomicInteger statusMessageCount = new AtomicInteger(0);
        processor.registerRecordsLoggedListener(statusMessage -> statusMessageCount.incrementAndGet());

        for (byte[] frame : extractFrames()) processor.onFrame(frame);

        assertEquals("The record count should not change when nothing is logging or streaming", 0, statusMessageCount.get());
    }

    private static List<byte[]> extractFrames() throws IOException
    {
        final DiagRevealerMessage diagRevealerMessage = ParserUtils.getNextDiagRevealerMessage(
                new BufferedInputStream(new ByteArrayInputStream(LTE_RRC_DIAG_REVEALER_MESSAGE)));
        assertNotNull(diagRevealerMessage);
        return new QcdmFrameParser().extractFrames(diagRevealerMessage);
    }
}
