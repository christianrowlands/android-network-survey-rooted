package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;
import com.craxiom.networksurveyplus.parser.FrameParser;
import com.craxiom.networksurveyplus.processor.MessageProcessor;
import com.craxiom.networksurveyplus.util.ParserUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import timber.log.Timber;

/**
 * A runnable that handles reading from the FIFO queue that the revealer daemon writes to.
 * This runnable handles reading from the queue, stripping off the revealer added headers,
 * and then passing off the bytes to a processor so that the modem messages can be processed.
 *
 * @since 0.1.0
 */
public class FifoReadRunnable implements Runnable
{
    private final String fifoPipeName;
    private final FrameParser frameParser;
    private final MessageProcessor messageProcessor;

    private volatile boolean done = false;

    /**
     * Constructs this runnable object so that it can read from the FIFO queue.
     *
     * @param fifoPipeName     The absolute path to the FIFO named pipe file.
     * @param frameParser      The frame parser that will extract frames from revealer messages.
     * @param messageProcessor The message processor that will convert frames to PCAP messages.
     */
    public FifoReadRunnable(String fifoPipeName, FrameParser frameParser, MessageProcessor messageProcessor)
    {
        this.fifoPipeName = fifoPipeName;
        this.frameParser = frameParser;
        this.messageProcessor = messageProcessor;
    }

    @Override
    public void run()
    {
        readFifoQueue();
    }

    /**
     * Tell this runnable that it needs to wrap up its work and shutdown.
     */
    public void shutdown()
    {
        done = true;
    }

    private void readFifoQueue()
    {
        Timber.i("Starting the FIFO Reader");

        try (final FileInputStream fileInputStream = new FileInputStream(fifoPipeName);
             final BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream))
        {
            while (!done)
            {
                notifyMessageProcessor(ParserUtils.getNextDiagRevealerMessage(bufferedInputStream));
            }
        } catch (FileNotFoundException e)
        {
            Timber.e(e, "Could not find the named pipe %s", fifoPipeName);
        } catch (IOException e)
        {
            Timber.e(e, "An IO error occurred when reading from the diag pipe");
        } catch (Exception e)
        {
            Timber.e(e, "Caught an unexpected exception when trying to read from the FIFO diag revealer queue");
        }
    }

    /**
     * If the provided message is not null, the message is processed through the frame parser
     * and then passed to the message processor.
     * <p>
     * Any exceptions that occur during the processing of the message are trapped here.
     *
     * @param message The message to pass along.
     */
    private void notifyMessageProcessor(DiagRevealerMessage message)
    {
        if (message == null)
        {
            return;
        }

        try
        {
            // Extract frames from the revealer message
            List<byte[]> frames = frameParser.extractFrames(message);

            // Process each validated frame
            for (byte[] frame : frames)
            {
                if (frameParser.validateFrame(frame))
                {
                    messageProcessor.onFrame(frame);
                } else
                {
                    Timber.w("Frame validation failed, skipping frame");
                }
            }
        } catch (Throwable e)
        {
            Timber.e(e, "Could not process revealer message through frame parser and message processor");
        }
    }
}
