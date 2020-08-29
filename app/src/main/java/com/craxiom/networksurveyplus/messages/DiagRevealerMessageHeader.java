package com.craxiom.networksurveyplus.messages;

import timber.log.Timber;

/**
 * Represents the header that the Diag Revealer C program adds on to the QCDM messages. This is the header for the
 * {@link DiagRevealerMessage}.
 * <p>
 * The {@code messageType} from this class indicates what the payload of the {@link DiagRevealerMessage} is. The message
 * type is a 2 byte field (little endian format). The different message types are as follows:
 * 1: Represents a QCDM "Log" message.
 * 2: Represents the start of a new log file (typically qmdl or mi2log).
 * 3. Represents the end of a log file.
 * <p>
 * See {@link DiagRevealerMessage} for more details on the diag revealer message format.
 *
 * @since 0.1.0
 */
public class DiagRevealerMessageHeader
{
    public final int messageType;
    public final int messageLength;

    /**
     * Constructs a new instance of the diag revealer message header.
     *
     * @param messageType   The message type (see the class javadoc for more details on the messageType field.
     * @param messageLength The length of the message (excludes the 4 byte header).
     */
    public DiagRevealerMessageHeader(int messageType, int messageLength)
    {
        this.messageType = messageType;
        this.messageLength = messageLength;
    }

    /**
     * Parses the header the Diag Revealer C program adds on to the QCDM messages.
     *
     * @param headerBytes The message header bytes.
     * @return null if the parsing was unsuccessful or the {@link DiagRevealerMessageHeader} object if a message could be parsed.
     */
    public static DiagRevealerMessageHeader parseDiagRevealerMessageHeader(byte[] headerBytes)
    {
        try
        {
            if (headerBytes.length < 4)
            {
                Timber.e("The provided header byte array must be at least 4 bytes long");
                return null;
            }

            final short messageType = ParserUtils.getShort(headerBytes, 0, java.nio.ByteOrder.LITTLE_ENDIAN);
            final short messageLength = ParserUtils.getShort(headerBytes, 2, java.nio.ByteOrder.LITTLE_ENDIAN);

            return new DiagRevealerMessageHeader(messageType, messageLength);
        } catch (Exception e)
        {
            Timber.e(e, "Could not parse an incoming diag_revelaer message header due to an exception.");
            return null;
        }
    }

    @Override
    public String toString()
    {
        return "DiagRevealerMessageHeader{" +
                "messageType=" + messageType +
                ", messageLength=" + messageLength +
                '}';
    }
}
