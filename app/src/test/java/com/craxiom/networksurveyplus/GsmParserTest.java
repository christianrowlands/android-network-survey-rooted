package com.craxiom.networksurveyplus;

import android.location.Location;

import com.craxiom.networksurveyplus.messages.PcapMessage;
import com.craxiom.networksurveyplus.messages.QcdmConstants;
import com.craxiom.networksurveyplus.parser.QcdmGsmParser;
import com.craxiom.networksurveyplus.messages.QcdmMessage;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for the GSM QCDM Parser that takes QCDM input and converts it to a PCAP record.
 *
 * @since 0.4.0
 */
public class GsmParserTest
{
    @Test
    public void testGsmQcdmMessage_signalingMessage()
    {
        final byte[] qcdmMessageBytes = {(byte) 0x10, (byte) 0x00, (byte) 0x26, (byte) 0x00, (byte) 0x26, (byte) 0x00, (byte) 0x2f, (byte) 0x51, (byte) 0xfd, (byte) 0x19, (byte) 0x53, (byte) 0x7b, (byte) 0x87, (byte) 0x62, (byte) 0xf4, (byte) 0x00, (byte) 0x81, (byte) 0x1b, (byte) 0x17, (byte) 0x49, (byte) 0x06, (byte) 0x1b, (byte) 0x00, (byte) 0x3e, (byte) 0x62, (byte) 0xf2, (byte) 0x20, (byte) 0x1c, (byte) 0x4e, (byte) 0xd0, (byte) 0x01, (byte) 0x0a, (byte) 0x15, (byte) 0x65, (byte) 0x44, (byte) 0xb8, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x1f, (byte) 0x01, (byte) 0x1b};
        final byte[] expectedQcdmMessagePayloadBytes = {(byte) 0x81, (byte) 0x1b, (byte) 0x17, (byte) 0x49, (byte) 0x06, (byte) 0x1b, (byte) 0x00, (byte) 0x3e, (byte) 0x62, (byte) 0xf2, (byte) 0x20, (byte) 0x1c, (byte) 0x4e, (byte) 0xd0, (byte) 0x01, (byte) 0x0a, (byte) 0x15, (byte) 0x65, (byte) 0x44, (byte) 0xb8, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x1f, (byte) 0x01, (byte) 0x1b};
        final byte[] expectedPcapRecordBytes = {(byte) 0x63, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x63, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0xe4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x32, (byte) 0x75, (byte) 0x14, (byte) 0x00, (byte) 0x02, (byte) 0xcf, (byte) 0x14, (byte) 0x00, (byte) 0x0e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x21, (byte) 0x05, (byte) 0x84, (byte) 0x01, (byte) 0x8f, (byte) 0x90, (byte) 0x35, (byte) 0x3f, (byte) 0x1d, (byte) 0x61, (byte) 0x6b, (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x11, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x12, (byte) 0x79, (byte) 0x12, (byte) 0x79, (byte) 0x00, (byte) 0x2f, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x04, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49, (byte) 0x06, (byte) 0x1b, (byte) 0x00, (byte) 0x3e, (byte) 0x62, (byte) 0xf2, (byte) 0x20, (byte) 0x1c, (byte) 0x4e, (byte) 0xd0, (byte) 0x01, (byte) 0x0a, (byte) 0x15, (byte) 0x65, (byte) 0x44, (byte) 0xb8, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x1f, (byte) 0x01, (byte) 0x1b};

        final QcdmMessage qcdmMessage = new QcdmMessage(qcdmMessageBytes, 0);
        assertEquals(16, qcdmMessage.getOpCode());
        assertEquals(QcdmConstants.GSM_RR_SIGNALING_MESSAGES, qcdmMessage.getLogType());
        assertArrayEquals("The qcdm message bytes did not match what was expected", expectedQcdmMessagePayloadBytes, qcdmMessage.getLogPayload());

        final Location location = new FakeLocation();
        location.setLatitude(41.4928645);
        location.setLongitude(-90.1333759);
        location.setAltitude(152.6591);

        final PcapMessage pcapMessage = QcdmGsmParser.convertGsmSignalingMessage(qcdmMessage, location);

        assertNotNull(pcapMessage);

        final byte[] pcapRecordBytes = pcapMessage.getPcapRecord();

        // Ignore the first 8 bytes since it contains the record timestamp.
        assertArrayEquals(expectedPcapRecordBytes, Arrays.copyOfRange(pcapRecordBytes, 8, pcapRecordBytes.length));
    }
}
