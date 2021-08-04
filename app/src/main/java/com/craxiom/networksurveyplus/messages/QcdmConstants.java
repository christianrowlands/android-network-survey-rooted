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

    // GSM Signaling
    public static final int GSM_RR_SIGNALING_MESSAGES = 0x512F;
    public static final int GSM_POWER_SCAN_C = 0x64; //Used to view the BA List power levels
    public static final int GSM_RR_CELL_INFORMATION_C = 0x134;

    // UMTS/WCDMA
    public static final int WCDMA_SEARCH_CELL_RESELECTION_RANK = 0x4005;
    public static final int WCDMA_RRC_STATES = 0x4125;
    public static final int WCDMA_CELL_ID = 0x4127;
    public static final int WCDMA_SIB = 0x412B;
    public static final int WCDMA_SIGNALING_MESSAGES = 0x412F;
    public static final int UMTS_NAS_GMM_STATE = 0x7130;
    public static final int UMTS_NAS_MM_STATE = 0x7131;
    public static final int UMTS_NAS_MM_REG_State = 0x7135;
    public static final int UMTS_NAS_OTA = 0x713A;
    public static final int UMTS_NAS_OTA_DSDS = 0x7B3A;

    // UMTS Channel Types
    public static final int UMTS_UL_CCCH = 0;
    public static final int UMTS_UL_DCCH = 1;
    public static final int UMTS_DL_CCCH = 2;
    public static final int UMTS_DL_DCCH = 3;
    public static final int UMTS_DL_BCCH_BCH = 4;
    public static final int UMTS_DL_BCCH_FACH = 5;
    public static final int UMTS_DL_PCCH = 6;
    public static final int UMTS_DL_MCCH = 7;
    public static final int UMTS_DL_MSCH = 8;
    public static final int UMTS_EXTENSION_SIB = 9;
    public static final int UMTS_SIB_CONTAINER = 10;

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

    // LTE Channel Types
    public static final int LTE_BCCH_DL_SCH = 2;
    public static final int LTE_PCCH = 4;
    public static final int LTE_DL_CCCH = 5;
    public static final int LTE_DL_DCCH = 6;
    public static final int LTE_UL_CCCH = 7;
    public static final int LTE_UL_DCCH = 8;
}
