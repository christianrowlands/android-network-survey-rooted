package com.craxiom.networksurveyplus.messages;

import org.codehaus.preon.annotation.BoundNumber;
import org.codehaus.preon.annotation.Slice;
import org.codehaus.preon.buffer.ByteOrder;

public class LogMessagePayload
{
    @BoundNumber(size = "64", byteOrder = ByteOrder.LittleEndian)
    public long timestamp;

    @Slice(size = "(packetRecordLength - 24) * 8")
    public byte[] payload;
}
