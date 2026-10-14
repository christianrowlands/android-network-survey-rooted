package com.craxiom.networksurveyplus.revealer;

import android.content.Context;

import com.craxiom.networksurveyplus.Constants;
import com.craxiom.networksurveyplus.FifoReadRunnable;
import com.craxiom.networksurveyplus.util.RootUtil;
import com.google.common.io.ByteStreams;

import java.io.InterruptedIOException;

import timber.log.Timber;

/**
 * Responsible for executing the shannon_revealer native application so that Shannon modem diagnostic
 * output from the /dev/umts_dm0 device can be written to the FIFO named pipe.
 * <p>
 * The shannon_revealer application writes the same DiagRevealer format header before each message,
 * allowing the existing {@link FifoReadRunnable} to process Shannon modem data without modification.
 * See {@link com.craxiom.networksurveyplus.messages.DiagRevealerMessageHeader} and
 * {@link com.craxiom.networksurveyplus.messages.DiagRevealerMessage} for more details.
 * <p>
 * This class is structured identically to {@link DiagRevealerRunnable} but targets Samsung Shannon
 * modems instead of Qualcomm QCDM modems.
 */
public class ShannonRevealerRunnable implements Runnable
{
    private final Context context;
    private final String fifoPipeName;

    private volatile boolean done = false;
    private Process process;

    /**
     * @param context      The context to use when getting the native lib directory.
     * @param fifoPipeName The name of the FIFO pipe to write the Shannon diagnostic output to.
     */
    public ShannonRevealerRunnable(Context context, String fifoPipeName)
    {
        this.context = context;
        this.fifoPipeName = fifoPipeName;
    }

    @Override
    public void run()
    {
        startShannonRevealer();
    }

    /**
     * Tell this runnable that it needs to wrap up its work and shutdown.
     */
    public void shutdown()
    {
        done = true;

        if (process != null)
        {
            process.destroy();
        }
    }

    /**
     * Creates the Shannon Revealer command and then executes it. The Shannon Revealer command will
     * be run as root so that it can get access to the /dev/umts_dm0 device.
     */
    private void startShannonRevealer()
    {
        Timber.i("Starting the Shannon Revealer");

        if (isShannonDeviceAvailable())
        {
            RootUtil.killProcesses(Constants.LIB_SHANNON_REVEALER_NAME);
            executeCommand(createShannonRevealerCommand(fifoPipeName));
        } else
        {
            Timber.e("Shannon device is not available for diagnostic monitoring.");
        }
    }

    /**
     * Checks if a Shannon modem device is available on this device.
     * Checks for both primary (/dev/umts_dm0) and alternative (/dev/umts_ipc0) device paths.
     *
     * @return True if a Shannon device is found and accessible, false otherwise.
     */
    private boolean isShannonDeviceAvailable()
    {
        try
        {
            // Check primary Shannon device path
            Process process = new ProcessBuilder("su", "-c", "test -e " + Constants.SHANNON_DEVICE_PATH).start();
            if (process.waitFor() == 0)
            {
                Timber.i("Shannon device found at %s", Constants.SHANNON_DEVICE_PATH);
                return true;
            }

            // Check alternative Shannon device path
            process = new ProcessBuilder("su", "-c", "test -e " + Constants.SHANNON_DEVICE_PATH_ALT).start();
            if (process.waitFor() == 0)
            {
                Timber.i("Shannon device found at %s", Constants.SHANNON_DEVICE_PATH_ALT);
                return true;
            }

            Timber.w("No Shannon device found at %s or %s", Constants.SHANNON_DEVICE_PATH, Constants.SHANNON_DEVICE_PATH_ALT);
            return false;
        } catch (Exception e)
        {
            Timber.e(e, "Error checking for Shannon device availability");
            return false;
        }
    }

    /**
     * Determines which Shannon device path to use based on availability.
     *
     * @return The path to the available Shannon device, or the primary path as default.
     */
    private String getShannonDevicePath()
    {
        try
        {
            // Check primary path first
            Process process = new ProcessBuilder("su", "-c", "test -e " + Constants.SHANNON_DEVICE_PATH).start();
            if (process.waitFor() == 0)
            {
                return Constants.SHANNON_DEVICE_PATH;
            }

            // Fall back to alternative path
            process = new ProcessBuilder("su", "-c", "test -e " + Constants.SHANNON_DEVICE_PATH_ALT).start();
            if (process.waitFor() == 0)
            {
                return Constants.SHANNON_DEVICE_PATH_ALT;
            }
        } catch (Exception e)
        {
            Timber.e(e, "Error determining Shannon device path");
        }

        // Default to primary path if checks fail
        return Constants.SHANNON_DEVICE_PATH;
    }

    /**
     * Create the command array that can be used to execute the shannon revealer executable binary.
     * <p>
     * The shannon_revealer binary is packaged as a shared library (.so) in the APK but is actually
     * an executable that can be run directly. It takes three arguments:
     * 1. A config file path (currently unused, can be /dev/null)
     * 2. The FIFO pipe path where it will write DiagRevealer formatted messages
     * 3. The Shannon device path (/dev/umts_dm0 or /dev/umts_ipc0) - optional, defaults to /dev/umts_dm0
     *
     * @param fifoPipeName The file name for the FIFO pipe that will receive the output from the
     *                     Shannon modem device.
     * @return The command that can be executed to kick off the shannon revealer.
     */
    private String[] createShannonRevealerCommand(String fifoPipeName)
    {
        final String shannonRevealer = context.getApplicationInfo().nativeLibraryDir + "/" + Constants.LIB_SHANNON_REVEALER_NAME;
        final String configFile = "/dev/null";  // Config file unused but required by shannon_revealer
        final String devicePath = getShannonDevicePath();

        Timber.i("Shannon Revealer binary: %s", shannonRevealer);
        Timber.i("Config file: %s", configFile);
        Timber.i("FIFO pipe: %s", fifoPipeName);
        Timber.i("Shannon device path: %s", devicePath);

        return new String[]{"su", "-c", "exec " + shannonRevealer + " " + configFile + " " + fifoPipeName + " " + devicePath};
    }

    /**
     * Executes the provided command and then waits for the process to complete. This method also logs
     * any error output.
     * <p>
     * If the process terminates and we're not shutting down, this method will attempt to restart
     * the shannon_revealer process to maintain continuous monitoring.
     *
     * @param command The command to execute.
     */
    private void executeCommand(String[] command)
    {
        try
        {
            Timber.i("Executing Shannon Revealer command: %s", String.join(" ", command));

            process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start();

            String stdErrLine = new String(ByteStreams.toByteArray(process.getErrorStream()));
            String stdOutLine = new String(ByteStreams.toByteArray(process.getInputStream()));
            if (!stdErrLine.isEmpty()) Timber.e("ERROR: %s", stdErrLine);
            if (!stdOutLine.isEmpty()) Timber.d("STDOUT: %s", stdOutLine);

            process.waitFor();

            Timber.i("Done executing the shannon revealer command and the process has returned with exit value %d", process.exitValue());
        } catch (InterruptedIOException e)
        {
            Timber.i("The shannon revealer process was interrupted");
        } catch (Exception e)
        {
            Timber.e(e, "Something went wrong when executing the shannon revealer command, restarting Shannon Revealer");
            if (!done) startShannonRevealer();
        }
    }
}
