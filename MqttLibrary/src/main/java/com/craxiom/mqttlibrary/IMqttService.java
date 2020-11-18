package com.craxiom.mqttlibrary;

import android.os.Binder;

import com.craxiom.mqttlibrary.connection.ConnectionState;
import com.craxiom.mqttlibrary.connection.MqttBrokerConnectionInfo;

public interface IMqttService
{
    public void registerMqttConnectionStateListener(IConnectionStateListener connectionStateListener);

    public void unregisterMqttConnectionStateListener(IConnectionStateListener connectionStateListener);

    public void attemptMqttConnectWithMdmConfig(boolean value);

    public void connectToMqttBroker(MqttBrokerConnectionInfo brokerConnectionInfo);

    public void disconnectFromMqttBroker();

    ConnectionState getMqttConnectionState();
}
