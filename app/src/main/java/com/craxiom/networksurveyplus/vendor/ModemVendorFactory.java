package com.craxiom.networksurveyplus.vendor;

import java.io.File;

import timber.log.Timber;

/**
 * Factory for detecting and creating the appropriate modem vendor implementation.
 * This is the single decision point for vendor selection.
 */
public class ModemVendorFactory
{
    /**
     * Detects modem vendor based on device file presence.
     * This is the ONE decision point for vendor selection in the application.
     *
     * @return The detected modem vendor implementation
     */
    public static IModemVendor detectVendor()
    {
        // Check Shannon devices first (more specific paths)
        if (new File("/dev/umts_dm0").exists() ||
                new File("/dev/umts_ipc0").exists())
        {
            Timber.i("Detected Samsung Shannon modem");
            return new ShannonModemVendor();
        }

        // Check Qualcomm device
        if (new File("/dev/diag").exists())
        {
            Timber.i("Detected Qualcomm modem at /dev/diag");
            return new QualcommModemVendor();
        }

        // Default to Qualcomm (most common)
        Timber.w("No modem device detected, defaulting to Qualcomm");
        return new QualcommModemVendor();
    }
}
