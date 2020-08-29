package com.craxiom.networksurveyplus.messages;

import org.codehaus.preon.annotation.BoundNumber;
import org.codehaus.preon.buffer.ByteOrder;

public class DiagRevealerMessageHeaderPreon
{
    @BoundNumber(size = "16", byteOrder = ByteOrder.LittleEndian)
    private int messageType;

    @BoundNumber(size = "16", byteOrder = ByteOrder.LittleEndian)
    private int messageLength;
}
