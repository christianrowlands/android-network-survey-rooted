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
    /**
     * I have not been able to find a lot of information on this QCDM prefix, but it appears to have this structure:
     * <pre>
     * ****************************************************
     * | Command ID | Unknown |  Dummy  | Subscription ID |
     * |   1 byte   | 1 byte  | 2 bytes |     4 bytes     |
     * ****************************************************
     * </pre>
     * The Subscription ID always seems to be 0x98, but I am not sure why.
     * <p>
     * The Subscription ID is in little endian format, and seems to be for multi-sim card devices. For example,
     * 01 00 00 00 would be for Subscription ID = 1, 02 00 00 00 would be for Subscription ID = 2 (SIM 2), etc.
     * <p>
     * If we do have multiple SIM cards, it would probably be a good idea to route the messages to two different pcap
     * files or use two different destination IP addresses in the pcap file, and two different MQTT streams, but for now
     * we will merge it all into one.
     */
    public static final byte[] QCDM_PREFIX = {(byte) 0x98, (byte) 0x01, (byte) 0x00, (byte) 0x00};
    public static final byte[] QCDM_PREFIX_SIM_1 = {(byte) 0x98, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    public static final int QCDM_FOOTER = 0x7e;

    /**
     * The message bytes. This is with the QCDM header (0x98, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00)
     * and footer (0x7e) removed.
     */
    public final byte[] messageBytes;
    private final int simId;

    /**
     * Constructs a new {@link QcdmMessage} object.
     *
     * @param messageBytes The message bytes. This is with the QCDM header (0x98, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00)
     *                     and footer (0x7e) removed.
     * @param simId        The Subscription ID associated with the QCDM message. This starts at 0, and increments for
     *                     each SIM card in the device.
     */
    public QcdmMessage(byte[] messageBytes, int simId)
    {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.messageBytes = messageBytes;
        this.simId = simId;
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
        // TODO Should we use the length instead?
        //   https://github.com/RUB-SysSec/mobile_sentinel/blob/7fb1083e9f7ae233487db76afff11d5633338097/app/src/main/python/parsers/qualcomm/qualcommparser.py#L290
        return Arrays.copyOfRange(messageBytes, 16, messageBytes.length);
    }

    /**
     * Gets the Subscription ID associated with this QCDM message. This is used to indicate which SIM Card /
     * Subscription this message came from.
     *
     * @return The Subscription ID associated with this message.
     * @since 0.2.0
     */
    public int getSimId()
    {
        return simId;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString()
    {
        return "QcdmMessage{" +
                "messageBytes=" + ParserUtils.convertBytesToHexString(messageBytes, 0, messageBytes.length) +
                '}';
    }
}
