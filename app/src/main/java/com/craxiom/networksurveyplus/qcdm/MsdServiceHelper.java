package com.craxiom.networksurveyplus.qcdm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

public class MsdServiceHelper
{
    private static final String TAG = "msd-service-helper";
    private final Context context;
    private final MyServiceConnection serviceConnection = new MyServiceConnection();
    private final MsdServiceCallback callback;
    public IMsdService mIMsdService;

    class MyMsdServiceCallbackStub extends IMsdServiceCallback.Stub
    {
        @Override
        public void stateChanged(final String reason) throws RemoteException
        {
            (new Handler(Looper.getMainLooper())).post(new Runnable()
            {
                @Override
                public void run()
                {
                    callback.stateChanged(StateChangedReason.valueOf(reason));
                }
            });
        }

        @Override
        public void internalError() throws RemoteException
        {
            System.exit(0);
        }
    }

    MyMsdServiceCallbackStub msdCallback = new MyMsdServiceCallbackStub();
    private boolean connected = false;
    private StringBuffer logDataBuffer;

    public MsdServiceHelper(Context context, MsdServiceCallback callback)
    {
        this.context = context;
        this.callback = callback;
        bootstrap();
    }

    private void bootstrap()
    {
        // TODO if(StartupActivity.isSNSNCompatible(context.getApplicationContext())){ //only start MsdService if we think that this device is compatible
        startService();
    }

    private void startService()
    {
        Intent intent = new Intent(context, MsdService.class);
        if (Build.VERSION.SDK_INT >= 26)
        {
            Log.d(TAG, "starting service in foreground...");
            context.startForegroundService(intent);
        } else
        {
            context.startService(intent);
        }
        bindService();
    }

    private void bindService()
    {
        Intent intent = new Intent(context, MsdService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public boolean isConnected()
    {
        return connected;
    }

    public boolean startRecording()
    {
        Log.i(TAG, "MsdServiceHelper.startRecording() called");

		/*if(!PermissionChecker.isAccessingCoarseLocationAllowed(context)){
            Log.w(TAG,"MsdServiceHelper.startRecording() not allowed - User did not grant ACCESS_COARSE_LOCATION permission");
			return false;
		}*/
        if (!connected)
        {
            Log.i(TAG, "MsdServiceHelper.isRecording(): Not connected => false");
            return false;
        }

        boolean result = false;
        try
        {
            result = mIMsdService.startRecording();
        } catch (Exception e)
        {
            handleFatalError("Exception in MsdServiceHelper.startRecording()", e);
        }
        Log.i(TAG, "MsdServiceHelper.startRecording() returns " + result);
        return result;
    }

    public boolean stopRecording()
    {
        Log.i(TAG, "MsdServiceHelper.stopRecording() called");
        if (!connected)
        {
            Log.i(TAG, "MsdServiceHelper.isRecording(): Not connected => false");
            return false;
        }
        boolean result = false;
        try
        {
            result = mIMsdService.stopRecording();
        } catch (RemoteException e)
        {
            handleFatalError("RemoteException while calling mIMsdService.isRecording() in MsdServiceHelper.startRecording()", e);
        }
        Log.i(TAG, "MsdServiceHelper.stopRecording() returns " + result);
        return result;
    }

    public boolean isRecording()
    {
        if (!connected)
        {
            Log.i(TAG, "MsdServiceHelper.isRecording(): Not connected => false");
            return false;
        }
        boolean result = false;
        try
        {
            result = mIMsdService.isRecording();
        } catch (RemoteException e)
        {
            handleFatalError("RemoteException while calling mIMsdService.isRecording() in MsdServiceHelper.startRecording()", e);
        }
        Log.i(TAG, "MsdServiceHelper.isRecording() returns " + result);
        return result;
    }

    class MyServiceConnection implements ServiceConnection
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.i(MsdService.TAG, "MsdServiceHelper.MyServiceConnection.onServiceConnected()");
            mIMsdService = IMsdService.Stub.asInterface(service);
            try
            {
                mIMsdService.registerCallback(msdCallback);
                boolean recording = mIMsdService.isRecording();
                Log.i(TAG, "Initial recording = " + recording);

                // Write pending log data when the service is connected
                if (logDataBuffer != null)
                {
                    mIMsdService.writeLog("START Pending messages:\n");
                    mIMsdService.writeLog(logDataBuffer.toString());
                    mIMsdService.writeLog("END Pending messages:\n");
                    logDataBuffer = null;
                }
            } catch (RemoteException e)
            {
                handleFatalError("RemoteException in MsdServiceHelper.MyServiceConnection.onServiceConnected()", e);
            }
            connected = true;
            callback.stateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.i(TAG, "MsdServiceHelper.MyServiceConnection.onServiceDisconnected() called");
            context.unbindService(this);
            mIMsdService = null;
            connected = false;
            startService();
            // Service connection was lost, so let's call recordingStopped
            callback.stateChanged(StateChangedReason.RECORDING_STATE_CHANGED);
        }
    }

    private void handleFatalError(String errorMsg, Exception e)
    {
        String msg = errorMsg;
        if (e != null)
        {
            msg += e.getClass().getCanonicalName() + ": " + e.getMessage();
        }
        Log.e(TAG, msg, e);
        callback.internalError(msg);
    }

    public void writeLog(String logData)
    {
        if (isConnected())
        {
            try
            {
                mIMsdService.writeLog(logData);
            } catch (RemoteException e)
            {
                handleFatalError("RemoteException in writeLog()", e);
            }
        } else
        {
            if (logDataBuffer == null)
            {
                logDataBuffer = new StringBuffer();
            }
            logDataBuffer.append(logData);
        }
    }

    public int getParserNetworkGeneration()
    {
        try
        {
            return mIMsdService.getParserNetworkGeneration();
        } catch (RemoteException e)
        {
            handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
            return 0;
        }
    }

    public void endExtraRecording(boolean upload)
    {
        try
        {
            mIMsdService.endExtraRecording(upload);
        } catch (RemoteException e)
        {
            handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
        }
    }

    public void startExtraRecording(String filename)
    {
        try
        {
            mIMsdService.startExtraRecording(filename);
        } catch (RemoteException e)
        {
            handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
        }
    }

    public void startActiveTest()
    {
        try
        {
            mIMsdService.startActiveTest();
        } catch (RemoteException e)
        {
            handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
        }
    }

    public void stopActiveTest()
    {
        try
        {
            mIMsdService.stopActiveTest();
        } catch (RemoteException e)
        {
            handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
        }
    }

    public int getDiagMsgCount()
    {
        try
        {
            return mIMsdService.getDiagMsgCount();
        } catch (RemoteException e)
        {
            handleFatalError("RemoteException in MsdServiceHelper.mIMsdService.getParserNetworkGeneration()", e);
            return 0;
        }
    }

    public void stopService()
    {
        try
        {
            if (connected)
            {
                mIMsdService.exitService();
                context.unbindService(serviceConnection);
            }
        } catch (Exception e)
        {
            handleFatalError("Exception in MsdServiceHelper.stopService()", e);
        }
    }

    public long getLastAnalysisTimeMs()
    {
        try
        {
            return mIMsdService.getLastAnalysisTimeMs();
        } catch (Exception e)
        {
            handleFatalError("Exception in MsdServiceHelper.getLastAnalysisTimeMs()", e);
            return 0;
        }
    }
}
