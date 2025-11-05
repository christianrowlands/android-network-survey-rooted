package com.craxiom.networksurveyplus.vendor;

import android.content.Context;

import com.craxiom.networksurveyplus.GpsListener;
import com.craxiom.networksurveyplus.parser.FrameParser;
import com.craxiom.networksurveyplus.processor.MessageProcessor;

/**
 * Interface for modem vendor implementations.
 * Each vendor provides their own frame parsing, message processing, and device configuration.
 */
public interface IModemVendor
{
    /**
     * @return The full vendor name (e.g., "Qualcomm QCDM")
     */
    String getName();

    /**
     * @return The short vendor identifier (e.g., "qcdm")
     */
    String getShortName();

    /**
     * @return Array of possible device paths this vendor uses (e.g., ["/dev/diag"])
     */
    String[] getDevicePaths();

    /**
     * @return The detected available device path, or null if none found
     */
    String getActiveDevicePath();

    /**
     * Creates a runnable that launches the vendor-specific revealer daemon.
     *
     * @param context  The Android context
     * @param fifoPath The path to the FIFO pipe for communication
     * @return A runnable that launches the daemon
     */
    Runnable createDaemonLauncher(Context context, String fifoPath);

    /**
     * Creates a vendor-specific frame parser.
     *
     * @return A frame parser for this vendor's protocol
     */
    FrameParser createFrameParser();

    /**
     * Creates a vendor-specific message processor.
     *
     * @param gpsListener The GPS listener for location data
     * @return A message processor for this vendor's protocol
     */
    MessageProcessor createMessageProcessor(GpsListener gpsListener);

    /**
     * @return True if this vendor requires root access
     */
    boolean requiresRootAccess();

    /**
     * @return The minimum SDK version required for this vendor
     */
    int getMinSdkVersion();
}
