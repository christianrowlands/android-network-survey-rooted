package com.craxiom.networksurveyplus.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.craxiom.networksurveyplus.Constants;
import com.craxiom.networksurveyplus.R;
import com.craxiom.networksurveyplus.databinding.FragmentHomeBinding;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HomeFragment extends Fragment
{
    private static final String LOG_TAG = HomeFragment.class.getSimpleName();

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        final View view = binding.getRoot();

        binding.startButton.setOnClickListener(v -> startDiagStuff());

        return view;

        /*homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        final Button startButton = root.findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> startDiagStuff());

        return root;*/
    }

    private void startDiagStuff()
    {
        Log.i(LOG_TAG, "Starting the Diag stuff");

        // TODO Add some code that validates that the su binary is present, and that we can use it. Also that /dev/diag works

        final String fifoPipeName = getContext().getFilesDir() + "/" + Constants.FIFO_PIPE;
        createNamedPipe(fifoPipeName);

        final String[] command = createDiagRevealerCommand(fifoPipeName);

        executeCommand(command);

        startFifoReader();
    }

    /**
     * Create the named pipe that is used as a FIFO queue. This queue is where the binary output
     * from the diag port is sent.
     *
     * @param fifoPipeName THe name of the pipe to create.
     */
    private void createNamedPipe(String fifoPipeName)
    {
        // TODO only create if it does not exist
        final String[] namedPipeCommand = {"su", "-c", "exec mknod " + fifoPipeName + " p"};

        try
        {
            final Process process = Runtime.getRuntime().exec(namedPipeCommand);
            final int exitValue = process.waitFor();
            if (exitValue != 0)
            {
                Log.e(LOG_TAG, "exit value of creating the named pipe: " + exitValue);
                try (BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream())))
                {
                    String stdErrLine;
                    while ((stdErrLine = stdErr.readLine()) != null)
                    {
                        Log.e(LOG_TAG, "mknod ERROR: " + stdErrLine);
                    }
                }
            } else
            {
                Log.d(LOG_TAG, "Successfully ran the mknod command to create the named pipe");
            }
        } catch (Exception e)
        {
            Log.e(LOG_TAG, "Could not execute the mknod command to create the named pipe", e);
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
        final String diagRevealer = getContext().getApplicationInfo().nativeLibraryDir + "/" + Constants.LIB_DIAG_REVEALER_NAME;

        return new String[]{"su", "-c", "exec " + diagRevealer + " " + getContext().getFilesDir() + "/" + getResources().getResourceEntryName(R.raw.rrc_filter_diag) + " " + fifoPipeName};
    }

    private void executeCommand(String[] command)
    {
        try
        {
            final Process process = Runtime.getRuntime().exec(command);

            // FIXME This is all just temporary code. Need to do a better job of logging stdErr and stdOut
            try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream())))
            {
                String stdErrLine;
                String stdOutLine = null;
                while ((stdErrLine = stdErr.readLine()) != null || (stdOutLine = stdOut.readLine()) != null)
                {
                    if (stdErrLine != null) Log.e(LOG_TAG, "ERROR: " + stdErrLine);
                    if (stdOutLine != null) Log.d(LOG_TAG, "STDOUT: " + stdOutLine);
                }
            }
        } catch (Exception e)
        {
            Log.e(LOG_TAG, "Could not execute the diag revealer command", e);
        }
    }

    private void startFifoReader()
    {

    }
}