package com.craxiom.networksurveyplus.vendor;

import android.content.Context;

import com.craxiom.networksurveyplus.GpsListener;
import com.craxiom.networksurveyplus.QcdmMessageProcessor;
import com.craxiom.networksurveyplus.parser.FrameParser;
import com.craxiom.networksurveyplus.parser.QcdmFrameParser;
import com.craxiom.networksurveyplus.processor.MessageProcessor;
import com.craxiom.networksurveyplus.revealer.DiagRevealerRunnable;

/**
 * Qualcomm QCDM modem vendor implementation.
 * Uses /dev/diag device and diag_revealer binary.
 */
public class QualcommModemVendor extends BaseModemVendor
{
    @Override
    public String getName()
    {
        return "Qualcomm QCDM";
    }

    @Override
    public String getShortName()
    {
        return "qcdm";
    }

    @Override
    public String[] getDevicePaths()
    {
        return new String[]{"/dev/diag"};
    }

    @Override
    public Runnable createDaemonLauncher(Context context, String fifoPath)
    {
        return new DiagRevealerRunnable(context, fifoPath);
    }

    @Override
    public FrameParser createFrameParser()
    {
        return new QcdmFrameParser();
    }

    @Override
    public MessageProcessor createMessageProcessor(GpsListener gpsListener)
    {
        return new QcdmMessageProcessor(gpsListener);
    }

    @Override
    public int getMinSdkVersion()
    {
        return 26;  // Android 8.0
    }
}
