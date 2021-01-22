package com.craxiom.networksurveyplus.messages;

/**
 * The constants that are used for the QCDM log messages.
 *
 * @since 0.1.0
 */
public final class QcdmConstants
{
    private QcdmConstants()
    {
    }

    // LTE RRC
    public static final int LOG_LTE_RRC_OTA_MSG_LOG_C = 0xb0c0;

    // LTE NAS
    public static final int LOG_LTE_NAS_ESM_SEC_OTA_IN_MSG = 0xb0e0; // ESM Secure Incoming message
    public static final int LOG_LTE_NAS_ESM_SEC_OTA_OUT_MSG = 0xb0e1; // ESM Secure Outgoing message
    public static final int LOG_LTE_NAS_ESM_OTA_IN_MSG = 0xb0e2; // ESM Plain Incoming message
    public static final int LOG_LTE_NAS_ESM_OTA_OUT_MSG = 0xb0e3; // ESM Plain Outgoing Message
    public static final int LOG_LTE_NAS_EMM_SEC_OTA_IN_MSG = 0xb0ea; // EMM Secure Incoming message
    public static final int LOG_LTE_NAS_EMM_SEC_OTA_OUT_MSG = 0xb0eb;// EMM Secure Outgoing message
    public static final int LOG_LTE_NAS_EMM_OTA_IN_MSG = 0xb0ec; // EMM Plain Incoming message
    public static final int LOG_LTE_NAS_EMM_OTA_OUT_MSG = 0xb0ed; // EMM Plain Outgoing Message

    public static final int LTE_BCCH_DL_SCH = 2;
    public static final int LTE_PCCH = 4;
    public static final int LTE_DL_CCCH = 5;
    public static final int LTE_DL_DCCH = 6;
    public static final int LTE_UL_CCCH = 7;
    public static final int LTE_UL_DCCH = 8;
}
