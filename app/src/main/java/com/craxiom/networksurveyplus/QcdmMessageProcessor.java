package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;
import com.craxiom.networksurveyplus.messages.ParserUtils;
import com.craxiom.networksurveyplus.messages.QcdmMessage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    /**
     * @return True if either the UI or a listener needs this survey record processor.  False if the UI is hidden and
     * there are not any listeners.
     */
    synchronized boolean isBeingUsed()
    {
        return !messageListeners.isEmpty();
    }

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
            boolean hasQcdmPrefix = false;
            // Check to see if we need to remove the QCDM prefix
            if (ByteBuffer.wrap(QcdmMessage.QCDM_PREFIX).equals(ByteBuffer.wrap(diagRevealerMessage.payload, 0, 8)))
            {
                hasQcdmPrefix = true;
            }

            final short expectedCrc = ParserUtils.getShort(diagRevealerMessage.payload, diagRevealerMessage.payload.length - 3, ByteOrder.LITTLE_ENDIAN);
            final short crc = ParserUtils.calculateCrc16X25(diagRevealerMessage.payload, diagRevealerMessage.payload.length - 3);

            Timber.i(diagRevealerMessage.toString()); // TODO Delete me

            if (crc != expectedCrc)
            {
                Timber.w("Invalid CRC found on a diag message expected=%s, actual=%s", Integer.toHexString(expectedCrc), Integer.toHexString(crc));
            } else
            {
                Timber.d("CRC Check passed!");
                final byte[] qcdmBytes = Arrays.copyOfRange(diagRevealerMessage.payload, hasQcdmPrefix ? 8 : 0, diagRevealerMessage.payload.length - 3);
                notifyQcdmMessageListeners(new QcdmMessage(qcdmBytes));
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
