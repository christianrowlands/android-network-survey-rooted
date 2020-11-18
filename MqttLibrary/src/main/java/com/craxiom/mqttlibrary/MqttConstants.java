package com.craxiom.mqttlibrary;

public class MqttConstants
{
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
