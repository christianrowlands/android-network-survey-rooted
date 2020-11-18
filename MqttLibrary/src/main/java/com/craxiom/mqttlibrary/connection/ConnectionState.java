package com.craxiom.mqttlibrary.connection;

/**
 * Represents the various states of the MQTT Broker Connection.
 *
 * @since 0.1.0
 */
public enum ConnectionState
{
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}
