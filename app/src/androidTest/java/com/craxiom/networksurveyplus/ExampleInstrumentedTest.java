package com.craxiom.networksurveyplus;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.craxiom.networksurveyplus.messages.DiagRevealerMessageHeaderPreon;

import org.codehaus.preon.Codec;
import org.codehaus.preon.Codecs;
import org.codehaus.preon.DecodingException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest
{
    @Test
    public void useAppContext()
    {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.craxiom.networksurveyplus", appContext.getPackageName());
    }

    @Test
    public void testPreonParsingForDiagHeader()
    {
        final byte[] headerBytes = {1, 0, 13, 9};
        final short expectedMessageType = 1;
        final short expectedMessageLength = 2317;

        final Codec<DiagRevealerMessageHeaderPreon> headerCodec = Codecs.create(DiagRevealerMessageHeaderPreon.class);
        final DiagRevealerMessageHeaderPreon headerPreon;
        try
        {
            headerPreon = Codecs.decode(headerCodec, headerBytes);
        } catch (DecodingException e)
        {
            fail("Could not decode the diag revealer header bytes");
            return;
        }

        assertEquals(expectedMessageType, headerPreon.messageType);
        assertEquals(expectedMessageLength, headerPreon.messageLength);
    }
}