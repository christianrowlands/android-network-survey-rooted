package com.craxiom.networksurveyplus;

import android.content.Context;

import com.google.common.io.ByteStreams;

import java.io.InterruptedIOException;

import timber.log.Timber;

/**
 * Responsible for executing the diag_revealer native application so that QCDM output from the /dev/diag
 * device can be written to the FIFO named pipe.
 * <p>
 * The diag_revealer application write a custom format header before each QCDM message. See
 * {@link com.craxiom.networksurveyplus.messages.DiagRevealerMessageHeader} and
 * {@link com.craxiom.networksurveyplus.messages.DiagRevealerMessage} for more details.
 *
 * @since 0.1.0
 */
public class DiagRevealerRunnable implements Runnable
{
    private final Context context;
    private final String fifoPipeName;

    private volatile boolean done = false;
    private Process process;

    /**
     * @param context      The context to use when getting the native lib directory.
     * @param fifoPipeName The name of the FIFO pipe to write the QCDM output to.
     */
    DiagRevealerRunnable(Context context, String fifoPipeName)
    {
        this.context = context;
        this.fifoPipeName = fifoPipeName;
    }

    @Override
    public void run()
    {
        startDiagRevealer();
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
     * Creates the Diag Revealer command and then executes it. The Diag Revealer command will be run as root
     * so that it can get access to the /dev/diag device.
     */
    private void startDiagRevealer()
    {
        Timber.i("Starting the Diag Revealer");

        if (RootUtil.isDeviceReadyForDiagReceiver())
        {
            executeCommand(createDiagRevealerCommand(fifoPipeName));
        } else
        {
            Timber.e("Device is not ready for diagnostic monitoring.");
        }
    }

    /**
     * Create the command array that can be used to execute the diag revealer executable binary.
     *
     * @param fifoPipeName The file name for the FIFO pipe that will receive the output from the
     *                     /dev/diag port.
     * @return The command that can be executed to kick off the diag revealer.
     */
    private String[] createDiagRevealerCommand(String fifoPipeName)
    {
        final String diagRevealer = context.getApplicationInfo().nativeLibraryDir + "/" + Constants.LIB_DIAG_REVEALER_NAME;

        return new String[]{"su", "-c", "exec " + diagRevealer + " " + context.getFilesDir() + "/" + context.getResources().getResourceEntryName(R.raw.rrc_filter_diag) + " " + fifoPipeName};
    }

    /**
     * Executes the provided command and then waits for the process to complete. This method also logs
     * any error output.
     *
     * @param command The command to execute.
     */
    private void executeCommand(String[] command)
    {
        try
        {
            process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start();

            String stdErrLine = new String(ByteStreams.toByteArray(process.getErrorStream()));
            String stdOutLine = new String(ByteStreams.toByteArray(process.getInputStream()));
            if (!stdErrLine.isEmpty()) Timber.e("ERROR: %s", stdErrLine);
            if (!stdOutLine.isEmpty()) Timber.d("STDOUT: %s", stdOutLine);

            process.waitFor();

            Timber.i("Done executing the diag revealer command and the process has returned with exit value %d", process.exitValue());
        } catch (InterruptedIOException e)
        {
            Timber.i("The diag revealer process was interrupted");
        } catch (Exception e)
        {
            Timber.e(e, "Something went wrong when executing the diag revealer command, restarting Diag Revealer");
            if (!done) startDiagRevealer();
        }
    }
}
