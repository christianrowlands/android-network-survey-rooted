package com.craxiom.networksurveyplus.vendor;

import android.content.Context;

import com.craxiom.networksurveyplus.GpsListener;
import com.craxiom.networksurveyplus.parser.FrameParser;
import com.craxiom.networksurveyplus.parser.ShannonFrameParser;
import com.craxiom.networksurveyplus.processor.MessageProcessor;
import com.craxiom.networksurveyplus.processor.ShannonMessageProcessor;
import com.craxiom.networksurveyplus.revealer.ShannonRevealerRunnable;

/**
 * Samsung Shannon modem vendor implementation.
 * Used by Samsung Galaxy S6+ devices and Google Pixel 6+ devices.
 * Uses /dev/umts_dm0 or /dev/umts_ipc0 device and shannon_revealer binary.
 */
public class ShannonModemVendor extends BaseModemVendor
{
    @Override
    public String getName()
    {
        return "Samsung Shannon";
    }

    @Override
    public String getShortName()
    {
        return "shannon";
    }

    @Override
    public String[] getDevicePaths()
    {
        // Shannon devices can be at either path
        return new String[]{"/dev/umts_dm0", "/dev/umts_ipc0"};
    }

    @Override
    public Runnable createDaemonLauncher(Context context, String fifoPath)
    {
        return new ShannonRevealerRunnable(context, fifoPath);
    }

    @Override
    public FrameParser createFrameParser()
    {
        return new ShannonFrameParser();
    }

    @Override
    public MessageProcessor createMessageProcessor(GpsListener gpsListener)
    {
        return new ShannonMessageProcessor(gpsListener);
    }

    @Override
    public int getMinSdkVersion()
    {
        return 26;  // Android 8.0+
    }
}
