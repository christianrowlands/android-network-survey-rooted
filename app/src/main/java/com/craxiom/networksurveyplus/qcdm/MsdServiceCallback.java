package com.craxiom.networksurveyplus.qcdm;

public interface MsdServiceCallback
{
    /**
     * Called when the recording state has changed for any reason. The UI can
     * then use isRecording to check whether the Service is actually recording.
     */
    void stateChanged(StateChangedReason reason);

    /**
     * Called when an internal error within MsdServiceHelper occurs. The UI
     * should show an error message and terminate itself.
     *
     * @param msg
     */
    void internalError(String msg);
}
