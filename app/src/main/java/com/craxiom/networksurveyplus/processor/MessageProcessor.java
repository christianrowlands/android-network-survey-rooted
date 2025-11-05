package com.craxiom.networksurveyplus.processor;

import com.craxiom.networksurveyplus.IPcapMessageListener;
import com.craxiom.networksurveyplus.IServiceStatusListener;

/**
 * Vendor-specific message processor that converts modem frames to PCAP messages.
 * Each vendor has its own message formats and processing logic.
 */
public interface MessageProcessor
{
    /**
     * Registers a listener for PCAP messages.
     *
     * @param listener The listener to add
     */
    void registerMessageListener(IPcapMessageListener listener);

    /**
     * Unregisters a PCAP message listener.
     *
     * @param listener The listener to remove
     */
    void unregisterMessageListener(IPcapMessageListener listener);

    /**
     * Registers a listener for service status updates (e.g., records logged count).
     *
     * @param listener The listener to add
     */
    void registerRecordsLoggedListener(IServiceStatusListener listener);

    /**
     * Unregisters a service status listener.
     *
     * @param listener The listener to remove
     */
    void unregisterRecordsLoggedListener(IServiceStatusListener listener);

    /**
     * Processes a single frame from the modem.
     * The frame has already been validated and unescaped by the FrameParser.
     *
     * @param frame The raw frame bytes to process
     */
    void onFrame(byte[] frame);

    /**
     * @return True if there are active listeners (UI or log consumers)
     */
    boolean isBeingUsed();
}
