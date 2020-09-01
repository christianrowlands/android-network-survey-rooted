package com.craxiom.networksurveyplus;

import android.content.Context;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;
import com.craxiom.networksurveyplus.messages.DiagRevealerMessageHeader;
import com.craxiom.networksurveyplus.messages.DiagRevealerMessageHeaderPreon;

import org.codehaus.preon.Codec;
import org.codehaus.preon.Codecs;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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
    private final Context context;
    private final String fifoPipeName;
    private final QcdmMessageProcessor qcdmMessageProcessor;

    private volatile boolean done = false;

    FifoReadRunnable(Context context, String fifoPipeName, QcdmMessageProcessor qcdmMessageProcessor)
    {
        this.context = context;
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
             final BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             /*final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()*/)
        {
            /* TODO This is how the other apps handle reading from the diag port. They use the 0x7e delimiter to mark
                the end of the message. However, I have a hard time believing that the actual data will never have a
                byte of 0x7e. Leaving this code if that is actually true or if we could simply put the bytes back
                if we can't parse the message, but that seems error prone. My concern about the other approach is that
                if we get out of sync with the message header, then we don't have a good way to get back in alignment
                with the messages. At least with the 0x7e delimiter approach we will always get back to parsing correctly
            int nextByte;
            while ((nextByte = bufferedInputStream.read()) != -1)
            {
                if (nextByte != QcdmMessage.QCDM_FOOTER)
                {
                    outputStream.write(nextByte);
                    continue;
                }

                // We have reached the QCDM message delimiter.
                final Codec<DiagRevealerMessagePreon> messageCodec = Codecs.create(DiagRevealerMessagePreon.class);
                final DiagRevealerMessagePreon message = Codecs.decode(messageCodec, outputStream.toByteArray());
                outputStream.reset();

                notifyMessageProcessor(message);
            }*/

            final byte[] headerBytes = new byte[4];
            while ((bufferedInputStream.read(headerBytes)) != -1 && !done)
            {
                // TODO Delete me
                final Codec<DiagRevealerMessageHeaderPreon> headerCodec = Codecs.create(DiagRevealerMessageHeaderPreon.class);
                final DiagRevealerMessageHeaderPreon headerPreon = Codecs.decode(headerCodec, headerBytes);

                // TODO Verify the number of bytes read is == 4

                final DiagRevealerMessageHeader header = DiagRevealerMessageHeader.parseDiagRevealerMessageHeader(headerBytes);
                if (header == null)
                {
                    // TODO do something about the header being null
                    Timber.i("The header is null");
                }

                final byte[] messageBytes = new byte[header.messageLength];

                final int bytesRead = bufferedInputStream.read(messageBytes);
                if (bytesRead != messageBytes.length)
                {
                    Timber.e("Could not get the correct number of bytes from the FIFO diag revealer queue; bytesRead=%d, expectedLength=%d", bytesRead, messageBytes.length);
                }

                notifyMessageProcessor(DiagRevealerMessage.parseDiagRevealerMessage(messageBytes, header));
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
