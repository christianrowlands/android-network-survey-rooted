package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.PcapMessage;

/**
 * Listener interface for those interested in being notified when a new diag revealer message has been processed into a
 * PCAP record.
 *
 * @since 0.1.0
 */
public interface IPcapMessageListener
{
    /**
     * Called when a new PCAP message is ready.
     *
     * @param pcapMessage the cellular OTA message stored in a PCAP record.
     */
    void onPcapMessage(PcapMessage pcapMessage);
}
