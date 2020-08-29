package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.QcdmMessage;

/**
 * Listener interface for those interested in being notified when a new diag revealer message is ready.
 *
 * @since 0.1.0
 */
public interface IQcdmMessageListener
{
    /**
     * Called when a new QCDM message is ready.
     *
     * @param qcdmMessage the QCDM message.
     */
    void onQcdmMessage(QcdmMessage qcdmMessage);
}
