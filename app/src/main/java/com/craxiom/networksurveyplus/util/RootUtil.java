//========================================================================
//
//                       U N C L A S S I F I E D
//
//========================================================================
//  Copyright (c) 2020 Chesapeake Technology International Corp.
//  ALL RIGHTS RESERVED
//  This material may be reproduced by or for the U.S. Government
//  pursuant to the copyright license under the clause at
//  DFARS 252.227-7013 (OCT 1988).
//=======================================================================

package com.craxiom.networksurveyplus.util;

import com.google.common.io.ByteStreams;

import timber.log.Timber;

import java.io.File;
import java.io.IOException;

/**
 * Responsible for validating that the phone has the 'su' binary on the PATH and that
 * we are able to use it to get root privileges. Additionally, this class validates
 * that the /dev/diag device is present and operational.
 * <p>
 * Some of the implementation in this class is based on this stack overflow page:
 * https://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
 *
 * @since 0.1.0
 */
public class RootUtil
{
    public static boolean isDeviceReadyForDiagReceiver()
    {
        return isRootPrivGiven() && isDevDiagAvailable();
    }

    /**
     * Kills any running processes (as root) for the provided native executable.
     * <p>
     * Destroying the {@link Process} for a "su -c" command only stops the su client, so the root process it started
     * can keep running after the app service is destroyed. A second copy then gets no data from the modem, so any
     * leftover process needs to be killed before starting a new one. This method blocks, so don't call it from the
     * main thread.
     *
     * @param executableName The file name of the native executable (e.g. libdiag_revealer.so).
     * @since 0.6.0
     */
    public static void killProcesses(String executableName)
    {
        // Matching the "." with "[.]" keeps the pattern from matching the shell that runs the pkill command
        final String pattern = executableName.replace(".", "[.]");
        try
        {
            Process process = new ProcessBuilder("su", "-c", "pkill -f '" + pattern + "'").start();
            if (process.waitFor() == 0) Timber.i("Killed a leftover %s process", executableName);
        } catch (Exception e)
        {
            Timber.e(e, "Could not kill the leftover %s processes", executableName);
        }
    }

    private static boolean isRootPrivGiven()
    {
        if (isRootAvailable())
        {
            Process process = null;
            try
            {
                process = new ProcessBuilder("su", "-c", "id")
                        .redirectInput(ProcessBuilder.Redirect.PIPE)
                        .start();
                String output = new String(ByteStreams.toByteArray(process.getInputStream()));
                return output.toLowerCase().contains("uid=0");
            } catch (IOException e)
            {
                Timber.e("su ERROR: %s", e.getMessage());
            } finally
            {
                if (process != null)
                {
                    process.destroy();
                }
            }
        }

        return false;
    }

    private static boolean isRootAvailable()
    {
        for (String pathDir : System.getenv("PATH").split(":"))
        {
            if (new File(pathDir, "su").exists())
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isDevDiagAvailable()
    {
        return isDevDiagReadable() && isDevDiagWritable();
    }

    private static boolean isDevDiagReadable()
    {
        try
        {
            Process process = new ProcessBuilder("su", "-c", "test -e /dev/diag").start();
            return process.waitFor() == 0;
        } catch (Exception e)
        {
            Timber.e("Reading /dev/diag ERROR: %s", e.getMessage());
        }
        return false;
    }

    private static boolean isDevDiagWritable()
    {
        try
        {
            Process process = new ProcessBuilder("su", "-c", "test -w /dev/diag").start();
            return process.waitFor() == 0;
        } catch (Exception e)
        {
            Timber.e("Writing to /dev/diag ERROR: %s", e.getMessage());
        }
        return false;
    }
}
