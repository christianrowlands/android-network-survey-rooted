package com.craxiom.networksurveyplus.messages;

import org.codehaus.preon.annotation.BoundObject;
import org.codehaus.preon.annotation.Choices;
import org.codehaus.preon.annotation.Slice;

/**
 * The Diag Revealer native application reads from the /dev/diag port, and writes the output to a FIFO named pipe. Diag
 * Revealer sends exactly what is received from /dev/diag, except that it also adds a header.
 * <p>
 * Following is the comment from the diag_revealer.c file and describes the custom format used by Diag Revealer:
 * <pre>
 * This program writes to FIFO using a special packet format:
 *    type: 2-byte integer. Can be one of the following values:
 *      1: LOG
 *      2: START_LOG_FILE, indicating the creation of a new log file.
 *      3: END_LOG_FILE, indicating the end of a log file.
 *    length: 2-byte integer. The total number of bytes in this packet
 *      (excluding the type field).
 * If "type" is LOG, there are two other fields:
 *    timestamp: 8-byte double float number. A POSIX timestamp representing
 *      when this log is received from the device.
 *    payload: byte stream of variable length.
 * Otherwise, "type" contains only one field:
 *    filename: the related log file's name
 * </pre>
 *
 * @since 0.1.0
 */
public class DiagRevealerMessagePreon
{
    @BoundObject(type = DiagRevealerMessageHeaderPreon.class)
    public DiagRevealerMessageHeaderPreon header;

    @Slice(size = "(packetRecordLength - 24) * 8")
    @BoundObject(selectFrom =
    @Choices(alternatives = {
            @Choices.Choice(condition = "header.messageType == 1", type = LogMessagePayload.class),
            @Choices.Choice(condition = "header.messageType == 2 || header.messageType == 3", type = LogFilePayload.class)}
    )
    )
    private Object payload;
}
