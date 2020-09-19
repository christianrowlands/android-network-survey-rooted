package com.craxiom.networksurveyplus;

/**
 * A generic service status message.
 */
public class ServiceStatusMessage
{
    // Message types
    public static final int SERVICE_LOCATION_MESSAGE = 1;
    public static final int SERVICE_RECORD_LOGGED_MESSAGE = 2;

    /**
     * The message identifier
     */
    public int what;

    /**
     * The data content of the message
     */
    public Object data;

    /**
     * Constructor.
     *
     * @param what The message type
     * @param data The data content of the message
     */
    public ServiceStatusMessage(int what, Object data)
    {
        this.what = what;
        this.data = data;
    }
}
