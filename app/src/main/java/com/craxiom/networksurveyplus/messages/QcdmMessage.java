package com.craxiom.networksurveyplus.messages;

import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Represents a QCDM message received from the /dev/diag port.
 * <p>
 * The general structure of the QCDM message is:
 * <pre>
 * ********************************************************************************************************
 * | OP Code | Pending Messages | Log Outer Length | Log Inner Length | Log Type | Log Time | Log Payload |
 * | 1 byte  |      1 byte      |      2 bytes     |      2 bytes     | 2 bytes  | 8 bytes  |      n      |
 * ********************************************************************************************************
 * </pre>
 *
 * @since 0.1.0
 */
public class QcdmMessage
{
    public static final byte[] QCDM_PREFIX = {(byte) 0x98, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00};
    public static final int QCDM_FOOTER = 0x7e;

    /**
     * The message bytes. This is with the QCDM header (0x98, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00)
     * and footer (0x7e) removed.
     */
    public final byte[] messageBytes;

    /**
     * Constructs a new {@link QcdmMessage} object.
     *
     * @param messageBytes The message bytes. This is with the QCDM header (0x98, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00)
     *                     and footer (0x7e) removed.
     */
    public QcdmMessage(byte[] messageBytes)
    {
        this.messageBytes = messageBytes;
    }

    /**
     * The Op Code that represents what kind of Diag message this is.
     *
     * @return The Op Code for this message.
     * @see DiagCommand for the op code command types.
     */
    public int getOpCode()
    {
        return messageBytes[0] & 0xFF;
    }

    /**
     * @return The log type in the payload of this message
     * @see QcdmConstants for some of the types of logs.
     */
    public int getLogType()
    {
        final short logType = ParserUtils.getShort(messageBytes, 6, ByteOrder.LITTLE_ENDIAN);

        // Need to account for the unsigned aspect of the log type 16 bit value when moving to an int
        return logType >= 0 ? logType : 0x10000 + logType;
    }

    /**
     * The payload of this QCDM message. This strips off the 16 byte header.
     *
     * @return The payload without the header.
     */
    public byte[] getLogPayload()
    {
        return Arrays.copyOfRange(messageBytes, 16, messageBytes.length); // TODO Should we use the length instead?
    }

    @Override
    public String toString()
    {
        return "QcdmMessage{" +
                "messageBytes=" + ParserUtils.convertBytesToHexString(messageBytes, 0, messageBytes.length) +//Arrays.toString(messageBytes) +
                '}';
    }
}
