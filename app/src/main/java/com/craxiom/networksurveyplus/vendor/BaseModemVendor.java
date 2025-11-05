package com.craxiom.networksurveyplus.vendor;

import java.io.File;

/**
 * Abstract base class for modem vendor implementations.
 * Provides common functionality like device detection.
 */
public abstract class BaseModemVendor implements IModemVendor
{
    protected String activeDevicePath;

    protected BaseModemVendor()
    {
        activeDevicePath = detectActiveDevice();
    }

    /**
     * Detects which device path from getDevicePaths() is actually available.
     *
     * @return The first available device path, or null if none found
     */
    protected String detectActiveDevice()
    {
        for (String path : getDevicePaths())
        {
            if (new File(path).exists())
            {
                return path;
            }
        }
        return null;
    }

    @Override
    public String getActiveDevicePath()
    {
        return activeDevicePath;
    }

    @Override
    public boolean requiresRootAccess()
    {
        return true;  // All vendors need root by default
    }
}
