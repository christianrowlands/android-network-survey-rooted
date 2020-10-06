package com.craxiom.networksurveyplus;

/**
 * A generic service status message that can be sent to any {@link IServiceStatusListener}s.
 */
public class ServiceStatusMessage
{
    // Message types
    public static final int SERVICE_LOCATION_MESSAGE = 1;
    public static final int SERVICE_RECORD_LOGGED_MESSAGE = 2;
    public static final int SERVICE_GPS_LOCATION_PROVIDER_STATUS = 3;

    /**
     * The message identifier.
     */
    public int what;

    /**
     * The data content of the message.
     */
    public Object data;

    /**
     * @param what The message type.
     * @param data The data content of the message.
     */
    public ServiceStatusMessage(int what, Object data)
    {
        this.what = what;
        this.data = data;
    }

    public enum LocationProviderStatus
    {
        GPS_PROVIDER_ENABLED,
        GPS_PROVIDER_DISABLED
    }
}
