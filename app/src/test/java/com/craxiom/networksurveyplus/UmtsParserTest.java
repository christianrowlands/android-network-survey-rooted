package com.craxiom.networksurveyplus;

import android.location.Location;

import com.craxiom.networksurveyplus.messages.QcdmConstants;
import com.craxiom.networksurveyplus.messages.QcdmMessage;
import com.craxiom.networksurveyplus.messages.QcdmUmtsParser;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UmtsParserTest
{
    @Test
    public void testUmtsQcdmMessage_nasOta()
    {
        final byte[] qcdmMessageBytes = {(byte) 0x10, (byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x3a, (byte) 0x71, (byte) 0xca, (byte) 0x54, (byte) 0x26, (byte) 0xe7, (byte) 0x6c, (byte) 0x60, (byte) 0xf3, (byte) 0x00, (byte) 0x01, (byte) 0x11, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x0c, (byte) 0x15, (byte) 0x05, (byte) 0xf4, (byte) 0xe8, (byte) 0xc0, (byte) 0xc5, (byte) 0x0f, (byte) 0x32, (byte) 0x02, (byte) 0x60, (byte) 0x00, (byte) 0x36, (byte) 0x02, (byte) 0x40, (byte) 0x00};
        final byte[] expectedQcdmMessagePayloadBytes = {(byte) 0x01, (byte) 0x11, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x0c, (byte) 0x15, (byte) 0x05, (byte) 0xf4, (byte) 0xe8, (byte) 0xc0, (byte) 0xc5, (byte) 0x0f, (byte) 0x32, (byte) 0x02, (byte) 0x60, (byte) 0x00, (byte) 0x36, (byte) 0x02, (byte) 0x40, (byte) 0x00};
        final byte[] expectedPcapRecordBytes = {(byte) 0x5d, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x5d, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0xe4, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x32, (byte) 0x75, (byte) 0x14, (byte) 0x00, (byte) 0x02, (byte) 0xcf, (byte) 0x14, (byte) 0x00, (byte) 0x0e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x21, (byte) 0x05, (byte) 0x84, (byte) 0x01, (byte) 0x8f, (byte) 0x90, (byte) 0x35, (byte) 0x3f, (byte) 0x1d, (byte) 0x61, (byte) 0x6b, (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x3d, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x11, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x12, (byte) 0x79, (byte) 0x12, (byte) 0x79, (byte) 0x00, (byte) 0x29, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x04, (byte) 0x02, (byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x0c, (byte) 0x15, (byte) 0x05, (byte) 0xf4, (byte) 0xe8, (byte) 0xc0, (byte) 0xc5, (byte) 0x0f, (byte) 0x32, (byte) 0x02, (byte) 0x60, (byte) 0x00, (byte) 0x36, (byte) 0x02, (byte) 0x40, (byte) 0x00};

        final QcdmMessage qcdmMessage = new QcdmMessage(qcdmMessageBytes, 0);
        assertEquals(16, qcdmMessage.getOpCode());
        assertEquals(QcdmConstants.UMTS_NAS_OTA, qcdmMessage.getLogType());
        assertArrayEquals("The qcdm message bytes did not match what was expected", expectedQcdmMessagePayloadBytes, qcdmMessage.getLogPayload());

        final Location location = new FakeLocation();
        location.setLatitude(41.4928645);
        location.setLongitude(-90.1333759);
        location.setAltitude(152.6591);

        final byte[] pcapRecordBytes = QcdmUmtsParser.convertUmtsNasOta(qcdmMessage, location);

        assertNotNull(pcapRecordBytes);

        // Ignore the first 8 bytes since it contains the record timestamp.
        assertArrayEquals(expectedPcapRecordBytes, Arrays.copyOfRange(pcapRecordBytes, 8, pcapRecordBytes.length));
    }
}
