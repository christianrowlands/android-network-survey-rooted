package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.mqtt.ConnectionState;

/**
 * Listener interface for those interested in keeping track of a connection state so they can update themselves
 * appropriately.
 *
 * @since 0.2.0
 */
public interface IConnectionStateListener
{
    /**
     * Called when the Connection State changes.
     *
     * @param newConnectionState the new Connection State.
     */
    void onConnectionStateChange(ConnectionState newConnectionState);
}
