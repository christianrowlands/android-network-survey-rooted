package com.craxiom.networksurveyplus.messages;

import org.codehaus.preon.annotation.Slice;

public class LogFilePayload
{
    @Slice(size = "(packetRecordLength - 24) * 8")
    private String fileName;
}
