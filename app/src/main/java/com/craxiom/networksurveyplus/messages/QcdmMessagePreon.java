package com.craxiom.networksurveyplus.messages;

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
public class QcdmMessagePreon
{
    public static final byte[] QCDM_PREFIX = {(byte) 0x98, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    public static final int QCDM_FOOTER = 0x7e;

    //@BoundNumber(size = "8")
    public int opCode;

    //@BoundNumber(size = "8")
    private int pendingMessages;

    //@BoundNumber(size = "16", byteOrder = org.codehaus.preon.buffer.ByteOrder.LittleEndian)
    private int logOuterLength;

    //@BoundNumber(size = "16", byteOrder = org.codehaus.preon.buffer.ByteOrder.LittleEndian)
    private int logInnerLength;

    //@BoundNumber(size = "16", byteOrder = org.codehaus.preon.buffer.ByteOrder.LittleEndian)
    public int logType;

    //@BoundNumber(size = "64", byteOrder = org.codehaus.preon.buffer.ByteOrder.LittleEndian)
    private int logTime;

    //@Slice(size = "logInnerlength * 8")
    public byte[] logPayload;

    @Override
    public String toString()
    {
        return "QcdmMessagePreon{" +
                "opCode=" + opCode +
                ", pendingMessages=" + pendingMessages +
                ", logOuterLength=" + logOuterLength +
                ", logInnerLength=" + logInnerLength +
                ", logType=" + logType +
                ", logTime=" + logTime +
                ", logPayload=" + ParserUtils.convertBytesToHexString(logPayload, 0, logPayload.length) +
                '}';
    }
}
