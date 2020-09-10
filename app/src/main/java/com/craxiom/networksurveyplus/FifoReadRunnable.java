package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;
import com.craxiom.networksurveyplus.messages.ParserUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import timber.log.Timber;

/**
 * A runnable that handles reading from the FIFO queue that the {@link DiagRevealerRunnable} write
 * to. This runnable handles reading from the queue, stripping off the diag_revealer added headers,
 * and then passing off the bytes to a processor so that the QCDM binary messages can be processed.
 *
 * @since 0.1.0
 */
public class FifoReadRunnable implements Runnable
{
    private final String fifoPipeName;
    private final QcdmMessageProcessor qcdmMessageProcessor;

    private volatile boolean done = false;

    /**
     * Constructs this runnable object so that it can read from the FIFO queue.
     *
     * @param fifoPipeName         The absolute path to the FIFO named pipe file.
     * @param qcdmMessageProcessor The message processor that will consume the QCDM messages coming
     *                             from the FIFO queue.
     */
    FifoReadRunnable(String fifoPipeName, QcdmMessageProcessor qcdmMessageProcessor)
    {
        this.fifoPipeName = fifoPipeName;
        this.qcdmMessageProcessor = qcdmMessageProcessor;
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
                final byte[] diagMessageBytes = ParserUtils.getNextDiagMessageBytes(bufferedInputStream);

                if (diagMessageBytes == null)
                {
                    Timber.e("Could not get the diag revealer message bytes from the FIFO named pipe.");
                    continue;
                }

                if (diagMessageBytes.length > 4)
                {
                    notifyMessageProcessor(DiagRevealerMessage.parseDiagRevealerMessage(diagMessageBytes));
                } else
                {
                    Timber.e("The Diag Revealer message length was <= 4, actual length=%d", diagMessageBytes.length);
                }
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
     * If the provided message is not null, the message is passed off to the {@link QcdmMessageProcessor}.
     * <p>
     * Any exceptions that occur during the processing of the message are trapped here.
     *
     * @param message The message to pass along.
     */
    private void notifyMessageProcessor(DiagRevealerMessage message)
    {
        try
        {
            if (message != null) qcdmMessageProcessor.onDiagRevealerMessage(message);
        } catch (Throwable e)
        {
            Timber.e(e, "Could not notify the QCDM message processor about a new diag revealer message");
        }
    }
}
