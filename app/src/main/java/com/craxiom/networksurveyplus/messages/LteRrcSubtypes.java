package com.craxiom.networksurveyplus.messages;

/**
 * The GSMTAP mapping for the LTE RRC subtypes. The ordinal value of these enums map to the value that is used in the
 * GSMTAP header, so don't change the order of the enum values, and don't insert any new values in the middle.
 *
 * @since 0.1.0
 */
public enum LteRrcSubtypes
{
    GSMTAP_LTE_RRC_SUB_DL_CCCH_Message,          // 0
    GSMTAP_LTE_RRC_SUB_DL_DCCH_Message,          // 1
    GSMTAP_LTE_RRC_SUB_UL_CCCH_Message,          // 2
    GSMTAP_LTE_RRC_SUB_UL_DCCH_Message,          // 3
    GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message,         // 4
    GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message,      // 5
    GSMTAP_LTE_RRC_SUB_PCCH_Message,             // 6
    GSMTAP_LTE_RRC_SUB_MCCH_Message,             // 7
    GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message_MBMS,    // 8
    GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message_BR,   // 9
    GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message_MBMS, // 10
    GSMTAP_LTE_RRC_SUB_SC_MCCH_Message,          // 11
    GSMTAP_LTE_RRC_SUB_SBCCH_SL_BCH_Message,     // 12
    GSMTAP_LTE_RRC_SUB_SBCCH_SL_BCH_Message_V2X, // 13
    GSMTAP_LTE_RRC_SUB_DL_CCCH_Message_NB,       // 14
    GSMTAP_LTE_RRC_SUB_DL_DCCH_Message_NB,       // 15
    GSMTAP_LTE_RRC_SUB_UL_CCCH_Message_NB,       // 16
    GSMTAP_LTE_RRC_SUB_UL_DCCH_Message_NB,       // 17
    GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message_NB,      // 18
    GSMTAP_LTE_RRC_SUB_BCCH_BCH_Message_TDD_NB,  // 19
    GSMTAP_LTE_RRC_SUB_BCCH_DL_SCH_Message_NB,   // 20
    GSMTAP_LTE_RRC_SUB_PCCH_Message_NB,          // 21
    GSMTAP_LTE_RRC_SUB_SC_MCCH_Message_NB        // 22
}
