package com.craxiom.networksurveyplus;

public class Constants
{
    private Constants()
    {
    }

    public static final String FIFO_PIPE = "diag_revealer_fifo";
    public static final String DIAG_REVEALER_NAME = "libdiag_revealer";
    public static final String LIB_DIAG_REVEALER_NAME = DIAG_REVEALER_NAME + ".so";

    public static final String NOTIFICATION_CHANNEL_ID = "network_survey_plus_notification";
    public static final int LOGGING_NOTIFICATION_ID = 1;

    public static final String PCAP_FILE_NAME_PREFIX = "nsp-";

    /**
     * The key for the Intent extra that indicates the {@link QcdmService} is being started at boot.
     */
    public static final String EXTRA_STARTED_AT_BOOT = "com.craxiom.networksurveyplus.extra.STARTED_AT_BOOT";

    public static final int DEFAULT_LOG_ROLLOVER_SIZE_MB = 5;

    // Preferences
    public static final String PROPERTY_LOCATION_REFRESH_RATE_MS = "location_refresh_rate_ms";
    // the following need to match the keys of preference items within network_survey_settings.xml
    // or more specifically in strings.xml
    public static final String PROPERTY_AUTO_START_PCAP_LOGGING = "auto_start_logging";
    public static final String PROPERTY_LOG_ROLLOVER_SIZE_MB = "log_rollover_size";
}
