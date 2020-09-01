package com.craxiom.networksurveyplus.messages;

public class LogMessagePayload
{
    //@BoundNumber(size = "64", byteOrder = ByteOrder.LittleEndian)
    public long timestamp;

    //@Slice(size = "(packetRecordLength - 24) * 8")
    public byte[] payload;
}
