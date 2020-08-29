package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;
import com.craxiom.networksurveyplus.messages.QcdmMessage;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import timber.log.Timber;

/**
 * Responsible for consuming {@link DiagRevealerMessage} objects, cleaning them up by removing any headers and footers,
 * and then notifying any listeners of the new message.
 *
 * @since 0.1.0
 */
public class QcdmMessageProcessor
{
    private final Set<IQcdmMessageListener> messageListeners = new CopyOnWriteArraySet<>();

    void registerQcdmMessageListener(IQcdmMessageListener qcdmMessageListener)
    {
        messageListeners.add(qcdmMessageListener);
    }

    void unregisterQcdmMessageListener(IQcdmMessageListener qcdmMessageListener)
    {
        messageListeners.remove(qcdmMessageListener);
    }

    /**
     * Called when a new diag revealer message is ready.
     *
     * @param diagRevealerMessage the message received from the diag revealer.
     */
    void onDiagRevealerMessage(DiagRevealerMessage diagRevealerMessage)
    {
        Timber.v("Incoming Diag Revealer Message: %s", diagRevealerMessage);

        processDiagRevealerMessage(diagRevealerMessage);
    }

    // TODO Javadoc
    private void processDiagRevealerMessage(DiagRevealerMessage diagRevealerMessage)
    {
        // No reason to process the message if we don't have any listeners
        if (!messageListeners.isEmpty())
        {
            // Validate that the last byte is 0x7e
            if (diagRevealerMessage.payload[diagRevealerMessage.payload.length - 1] == QcdmMessage.QCDM_FOOTER)
            {
                boolean hasQcdmPrefix = false;
                // Check to see if we need to remove the QCDM prefix
                if (ByteBuffer.wrap(QcdmMessage.QCDM_PREFIX).equals(ByteBuffer.wrap(diagRevealerMessage.payload, 0, 8)))
                {
                    hasQcdmPrefix = true;
                }

                // TODO Need to validate the CRC
                final byte[] qcdmBytes = Arrays.copyOfRange(diagRevealerMessage.payload, hasQcdmPrefix ? 8 : 0, diagRevealerMessage.payload.length - 3);

                notifyQcdmMessageListeners(new QcdmMessage(qcdmBytes));
            } else
            {
                Timber.d("Throwing away a diag revealer message because it does not have the QCDM footer");
            }
        }
    }

    /**
     * Notify all the listeners that we have a new QCDM Record available.
     *
     * @param message The new QCDM message to send to the listeners.
     */
    private void notifyQcdmMessageListeners(QcdmMessage message)
    {
        if (message == null) return;
        for (IQcdmMessageListener listener : messageListeners)
        {
            try
            {
                listener.onQcdmMessage(message);
            } catch (Exception e)
            {
                Timber.e(e, "Unable to notify a QCDM Message Listener because of an exception");
            }
        }
    }
}
