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
    public static final String PROPERTY_MQTT_MDM_OVERRIDE = "mqtt_mdm_override";

    public static final boolean DEFAULT_MQTT_TLS_SETTING = true;
    public static final int MQTT_PLAIN_TEXT_PORT = 1883;
    public static final int MQTT_SSL_PORT = 8883;
    public static final int DEFAULT_MQTT_PORT = MQTT_SSL_PORT;

    // The following keys are used in the app_restrictions.xml file and also are settings stored in the app's shared preferences
    public static final String PROPERTY_MQTT_CONNECTION_HOST = "mqtt_connection_host";
    public static final String PROPERTY_MQTT_CONNECTION_PORT = "mqtt_connection_port";
    public static final String PROPERTY_MQTT_CLIENT_ID = "mqtt_client_id";
    public static final String PROPERTY_MQTT_CONNECTION_TLS_ENABLED = "mqtt_tls_enabled";
    public static final String PROPERTY_MQTT_USERNAME = "mqtt_username";
    public static final String PROPERTY_MQTT_PASSWORD = "mqtt_password";
}
