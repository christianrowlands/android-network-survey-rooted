package com.craxiom.networksurveyplus.messages;

/**
 * This class is created from http://cgit.osmocom.org/osmo-qcdiag/tree/src/protocol/diagcmd.h
 * <p>
 * The file header from Osmocom:
 * <pre>
 *     This file comes from vendor/qcom-proprietary/diag/src/diagcmd.h
 *     Don't change previous defines and add new id at the end
 *
 *     Command Codes between the Diagnostic Monitor and the mobile. Packets
 *     travelling in each direction are defined here, while the packet templates
 *     for requests and responses are distinct.  Note that the same packet id
 *     value can be used for both a request and a response.  These values
 *     are used to index a dispatch table in diag.c, so
 *
 *     DON'T CHANGE THE NUMBERS ( REPLACE UNUSED IDS WITH FILLERS ). NEW IDs
 *     MUST BE ASSIGNED AT THE END.
 * </pre>
 *
 * @since 0.1.0
 */
public final class DiagCommand
{
    private DiagCommand()
    {
    }

    /**
     * Version Number Request/Response
     */
    public static final int DIAG_VERNO_F = 0;

    /**
     * Mobile Station ESN Request/Response
     */
    public static final int DIAG_ESN_F = 1;

    /**
     * Peek byte Request/Response
     */
    public static final int DIAG_PEEKB_F = 2;

    /**
     * Peek word Request/Response
     */
    public static final int DIAG_PEEKW_F = 3;

    /**
     * Peek dword Request/Response
     */
    public static final int DIAG_PEEKD_F = 4;

    /**
     * Poke byte Request/Response
     */
    public static final int DIAG_POKEB_F = 5;

    /**
     * Poke word Request/Response
     */
    public static final int DIAG_POKEW_F = 6;

    /**
     * Poke dword Request/Response
     */
    public static final int DIAG_POKED_F = 7;

    /**
     * Byte output Request/Response
     */
    public static final int DIAG_OUTP_F = 8;

    /**
     * Word output Request/Response
     */
    public static final int DIAG_OUTPW_F = 9;

    /**
     * Byte input Request/Response
     */
    public static final int DIAG_INP_F = 10;

    /**
     * Word input Request/Response
     */
    public static final int DIAG_INPW_F = 11;

    /**
     * DMSS status Request/Response
     */
    public static final int DIAG_STATUS_F = 12;

    /** 13-14 Reserved */

    /**
     * Set logging mask Request/Response
     */
    public static final int DIAG_LOGMASK_F = 15;

    /**
     * Log packet Request/Response
     */
    public static final int DIAG_LOG_F = 16;

    /**
     * Peek at NV memory Request/Response
     */
    public static final int DIAG_NV_PEEK_F = 17;

    /**
     * Poke at NV memory Request/Response
     */
    public static final int DIAG_NV_POKE_F = 18;

    /**
     * Invalid Command Response
     */
    public static final int DIAG_BAD_CMD_F = 19;

    /**
     * Invalid parmaeter Response
     */
    public static final int DIAG_BAD_PARM_F = 20;

    /**
     * Invalid packet length Response
     */
    public static final int DIAG_BAD_LEN_F = 21;

    /** 22-23 Reserved */

    /**
     * Packet not allowed in this mode
     * ( online vs offline )
     */
    public static final int DIAG_BAD_MODE_F = 24;

    /**
     * info for TA power and voice graphs
     */
    public static final int DIAG_TAGRAPH_F = 25;

    /**
     * Markov statistics
     */
    public static final int DIAG_MARKOV_F = 26;

    /**
     * Reset of Markov statistics
     */
    public static final int DIAG_MARKOV_RESET_F = 27;

    /**
     * Return diag version for comparison to
     * detect incompatabilities
     */
    public static final int DIAG_DIAG_VER_F = 28;

    /**
     * Return a timestamp
     */
    public static final int DIAG_TS_F = 29;

    /**
     * Set TA parameters
     */
    public static final int DIAG_TA_PARM_F = 30;

    /**
     * Request for msg report
     */
    public static final int DIAG_MSG_F = 31;

    /**
     * Handset Emulation -- keypress
     */
    public static final int DIAG_HS_KEY_F = 32;

    /**
     * Handset Emulation -- lock or unlock
     */
    public static final int DIAG_HS_LOCK_F = 33;

    /**
     * Handset Emulation -- display request
     */
    public static final int DIAG_HS_SCREEN_F = 34;

    /** 35 Reserved */

    /**
     * Parameter Download
     */
    public static final int DIAG_PARM_SET_F = 36;

    /** 37 Reserved */

    /**
     * Read NV item
     */
    public static final int DIAG_NV_READ_F = 38;
    /**
     * Write NV item
     */
    public static final int DIAG_NV_WRITE_F = 39;
    /** 40 Reserved */

    /**
     * Mode change request
     */
    public static final int DIAG_CONTROL_F = 41;

    /**
     * Error record retreival
     */
    public static final int DIAG_ERR_READ_F = 42;

    /**
     * Error record clear
     */
    public static final int DIAG_ERR_CLEAR_F = 43;

    /**
     * Symbol error rate counter reset
     */
    public static final int DIAG_SER_RESET_F = 44;

    /**
     * Symbol error rate counter report
     */
    public static final int DIAG_SER_REPORT_F = 45;

    /**
     * Run a specified test
     */
    public static final int DIAG_TEST_F = 46;

    /**
     * Retreive the current dip switch setting
     */
    public static final int DIAG_GET_DIPSW_F = 47;

    /**
     * Write new dip switch setting
     */
    public static final int DIAG_SET_DIPSW_F = 48;

    /**
     * Start/Stop Vocoder PCM loopback
     */
    public static final int DIAG_VOC_PCM_LB_F = 49;

    /**
     * Start/Stop Vocoder PKT loopback
     */
    public static final int DIAG_VOC_PKT_LB_F = 50;

    /** 51-52 Reserved */

    /**
     * Originate a call
     */
    public static final int DIAG_ORIG_F = 53;
    /**
     * End a call
     */
    public static final int DIAG_END_F = 54;
    /** 55-57 Reserved */

    /**
     * Switch to downloader
     */
    public static final int DIAG_DLOAD_F = 58;
    /**
     * Test Mode Commands and FTM commands
     */
    public static final int DIAG_TMOB_F = 59;
    /**
     * Test Mode Commands and FTM commands
     */
    public static final int DIAG_FTM_CMD_F = 59;
    /** 60-62 Reserved */

    /**
     * Return the current state of the phone
     */
    public static final int DIAG_STATE_F = 63;

    /**
     * Return all current sets of pilots
     */
    public static final int DIAG_PILOT_SETS_F = 64;

    /**
     * Send the Service Prog. Code to allow SP
     */
    public static final int DIAG_SPC_F = 65;

    /**
     * Invalid nv_read/write because SP is locked
     */
    public static final int DIAG_BAD_SPC_MODE_F = 66;

    /**
     * get parms obsoletes PARM_GET
     */
    public static final int DIAG_PARM_GET2_F = 67;

    /**
     * Serial mode change Request/Response
     */
    public static final int DIAG_SERIAL_CHG_F = 68;

    /** 69 Reserved */

    /**
     * Send password to unlock secure operations
     * the phone to be in a security state that
     * is wasn't - like unlocked.
     */
    public static final int DIAG_PASSWORD_F = 70;

    /**
     * An operation was attempted which required
     */
    public static final int DIAG_BAD_SEC_MODE_F = 71;

    /**
     * Write Preferred Roaming list to the phone.
     */
    public static final int DIAG_PR_LIST_WR_F = 72;

    /**
     * Read Preferred Roaming list from the phone.
     */
    public static final int DIAG_PR_LIST_RD_F = 73;

    /** 74 Reserved */

    /**
     * Subssytem dispatcher (extended diag cmd)
     */
    public static final int DIAG_SUBSYS_CMD_F = 75;

    /** 76-80 Reserved */

    /**
     * Asks the phone what it supports
     */
    public static final int DIAG_FEATURE_QUERY_F = 81;

    /** 82 Reserved */

    /**
     * Read SMS message out of NV
     */
    public static final int DIAG_SMS_READ_F = 83;

    /**
     * Write SMS message into NV
     */
    public static final int DIAG_SMS_WRITE_F = 84;

    /**
     * info for Frame Error Rate
     * on multiple channels
     */
    public static final int DIAG_SUP_FER_F = 85;

    /**
     * Supplemental channel walsh codes
     */
    public static final int DIAG_SUP_WALSH_CODES_F = 86;

    /**
     * Sets the maximum # supplemental
     * channels
     */
    public static final int DIAG_SET_MAX_SUP_CH_F = 87;

    /**
     * get parms including SUPP and MUX2:
     * obsoletes PARM_GET and PARM_GET_2
     */
    public static final int DIAG_PARM_GET_IS95B_F = 88;

    /**
     * Performs an Embedded File System
     * (EFS) operation.
     */
    public static final int DIAG_FS_OP_F = 89;

    /**
     * AKEY Verification.
     */
    public static final int DIAG_AKEY_VERIFY_F = 90;

    /**
     * Handset emulation - Bitmap screen
     */
    public static final int DIAG_BMP_HS_SCREEN_F = 91;

    /**
     * Configure communications
     */
    public static final int DIAG_CONFIG_COMM_F = 92;

    /**
     * Extended logmask for > 32 bits.
     */
    public static final int DIAG_EXT_LOGMASK_F = 93;

    /** 94-95 reserved */

    /**
     * Static Event reporting.
     */
    public static final int DIAG_EVENT_REPORT_F = 96;

    /**
     * Load balancing and more!
     */
    public static final int DIAG_STREAMING_CONFIG_F = 97;

    /**
     * Parameter retrieval
     */
    public static final int DIAG_PARM_RETRIEVE_F = 98;

    /**
     * A state/status snapshot of the DMSS.
     */
    public static final int DIAG_STATUS_SNAPSHOT_F = 99;

    /**
     * Used for RPC
     */
    public static final int DIAG_RPC_F = 100;

    /**
     * Get_property requests
     */
    public static final int DIAG_GET_PROPERTY_F = 101;

    /**
     * Put_property requests
     */
    public static final int DIAG_PUT_PROPERTY_F = 102;

    /**
     * Get_guid requests
     */
    public static final int DIAG_GET_GUID_F = 103;

    /**
     * Invocation of user callbacks
     */
    public static final int DIAG_USER_CMD_F = 104;

    /**
     * Get permanent properties
     */
    public static final int DIAG_GET_PERM_PROPERTY_F = 105;

    /**
     * Put permanent properties
     */
    public static final int DIAG_PUT_PERM_PROPERTY_F = 106;

    /**
     * Permanent user callbacks
     */
    public static final int DIAG_PERM_USER_CMD_F = 107;

    /**
     * GPS Session Control
     */
    public static final int DIAG_GPS_SESS_CTRL_F = 108;

    /**
     * GPS search grid
     */
    public static final int DIAG_GPS_GRID_F = 109;

    /**
     * GPS Statistics
     */
    public static final int DIAG_GPS_STATISTICS_F = 110;

    /**
     * Packet routing for multiple instances of diag
     */
    public static final int DIAG_ROUTE_F = 111;

    /**
     * IS2000 status
     */
    public static final int DIAG_IS2000_STATUS_F = 112;

    /**
     * RLP statistics reset
     */
    public static final int DIAG_RLP_STAT_RESET_F = 113;

    /**
     * (S)TDSO statistics reset
     */
    public static final int DIAG_TDSO_STAT_RESET_F = 114;

    /**
     * Logging configuration packet
     */
    public static final int DIAG_LOG_CONFIG_F = 115;

    /**
     * Static Trace Event reporting
     */
    public static final int DIAG_TRACE_EVENT_REPORT_F = 116;

    /**
     * SBI Read
     */
    public static final int DIAG_SBI_READ_F = 117;

    /**
     * SBI Write
     */
    public static final int DIAG_SBI_WRITE_F = 118;

    /**
     * SSD Verify
     */
    public static final int DIAG_SSD_VERIFY_F = 119;

    /**
     * Log on Request
     */
    public static final int DIAG_LOG_ON_DEMAND_F = 120;

    /**
     * Request for extended msg report
     */
    public static final int DIAG_EXT_MSG_F = 121;

    /**
     * ONCRPC diag packet
     */
    public static final int DIAG_ONCRPC_F = 122;

    /**
     * Diagnostics protocol loopback.
     */
    public static final int DIAG_PROTOCOL_LOOPBACK_F = 123;

    /**
     * Extended build ID text
     */
    public static final int DIAG_EXT_BUILD_ID_F = 124;

    /**
     * Request for extended msg report
     */
    public static final int DIAG_EXT_MSG_CONFIG_F = 125;

    /**
     * Extended messages in terse format
     */
    public static final int DIAG_EXT_MSG_TERSE_F = 126;

    /**
     * Translate terse format message identifier
     */
    public static final int DIAG_EXT_MSG_TERSE_XLATE_F = 127;

    /**
     * Subssytem dispatcher Version 2 (delayed response capable)
     */
    public static final int DIAG_SUBSYS_CMD_VER_2_F = 128;

    /**
     * Get the event mask
     */
    public static final int DIAG_EVENT_MASK_GET_F = 129;

    /**
     * Set the event mask
     */
    public static final int DIAG_EVENT_MASK_SET_F = 130;

    /** RESERVED CODES: 131-139 */

    /**
     * Command Code for Changing Port Settings
     */
    public static final int DIAG_CHANGE_PORT_SETTINGS = 140;

    /**
     * Country network information for assisted dialing
     */
    public static final int DIAG_CNTRY_INFO_F = 141;

    /**
     * Send a Supplementary Service Request
     */
    public static final int DIAG_SUPS_REQ_F = 142;

    /**
     * Originate SMS request for MMS
     */
    public static final int DIAG_MMS_ORIG_SMS_REQUEST_F = 143;

    /**
     * Change measurement mode
     */
    public static final int DIAG_MEAS_MODE_F = 144;

    /**
     * Request measurements for HDR channels
     */
    public static final int DIAG_MEAS_REQ_F = 145;

    /**
     * Send Optimized F3 messages
     */
    public static final int DIAG_QSR_EXT_MSG_TERSE_F = 146;

    /**
     * LGE_CHANGES_S [minjong.gong@lge.com] 2010-06-11, LG_FW_DIAG_SCREEN_CAPTURE
     */
    public static final int DIAG_LGF_SCREEN_SHOT_F = 150;
    /**
     * LGE_CHANGES_E [minjong.gong@lge.com] 2010-06-11, LG_FW_DIAG_SCREEN_CAPTURE
     */

    // LG_FW : 2011.07.07 moon.yongho : saving webdload status variable to eMMC. ----------[[
    public static final int DIAG_WEBDLOAD_COMMON_F = 239;
    /**
     * ==>  0xef
     */
    // LG_FW : 2011.07.07 moon.yongho -------------------------------------------]]
    public static final int DIAG_WIFI_MAC_ADDR = 214;

    /**
     * Number of packets defined.
     */
    public static final int DIAG_TEST_MODE_F = 250;

    public static final int DIAG_SMS_TEST_F = 220;

    public static final int DIAG_LCD_Q_TEST_F = 253;
    public static final int DIAG_ERI_CMD_F = 254;

    public static final int DIAG_MAX_F = 255;

    public static final int LG_DIAG_CMD_LINE_LEN = 256;

    // TODO Is this enum needed?
//enum DiagSubsystem {
//  DIAG_SUBSYS_OEM                = 0,       /* Reserved for OEM use */
//  DIAG_SUBSYS_ZREX               = 1,       /* ZREX */
//  DIAG_SUBSYS_SD                 = 2,       /* System Determination */
//  DIAG_SUBSYS_BT                 = 3,       /* Bluetooth */
//  DIAG_SUBSYS_WCDMA              = 4,       /* WCDMA */
//  DIAG_SUBSYS_HDR                = 5,       /* 1xEvDO */
//  DIAG_SUBSYS_DIABLO             = 6,       /* DIABLO */
//  DIAG_SUBSYS_TREX               = 7,       /* TREX - Off-target testing environments */
//  DIAG_SUBSYS_GSM                = 8,       /* GSM */
//  DIAG_SUBSYS_UMTS               = 9,       /* UMTS */
//  DIAG_SUBSYS_HWTC               = 10,      /* HWTC */
//  DIAG_SUBSYS_FTM                = 11,      /* Factory Test Mode */
//  DIAG_SUBSYS_REX                = 12,      /* Rex */
//  DIAG_SUBSYS_OS                 = DIAG_SUBSYS_REX,
//  DIAG_SUBSYS_GPS                = 13,      /* Global Positioning System */
//  DIAG_SUBSYS_WMS                = 14,      /* Wireless Messaging Service (WMS, SMS) */
//  DIAG_SUBSYS_CM                 = 15,      /* Call Manager */
//  DIAG_SUBSYS_HS                 = 16,      /* Handset */
//  DIAG_SUBSYS_AUDIO_SETTINGS     = 17,      /* Audio Settings */
//  DIAG_SUBSYS_DIAG_SERV          = 18,      /* DIAG Services */
//  DIAG_SUBSYS_FS                 = 19,      /* File System - EFS2 */
//  DIAG_SUBSYS_PORT_MAP_SETTINGS  = 20,      /* Port Map Settings */
//  DIAG_SUBSYS_MEDIAPLAYER        = 21,      /* QCT Mediaplayer */
//  DIAG_SUBSYS_QCAMERA            = 22,      /* QCT QCamera */
//  DIAG_SUBSYS_MOBIMON            = 23,      /* QCT MobiMon */
//  DIAG_SUBSYS_GUNIMON            = 24,      /* QCT GuniMon */
//  DIAG_SUBSYS_LSM                = 25,      /* Location Services Manager */
//  DIAG_SUBSYS_QCAMCORDER         = 26,      /* QCT QCamcorder */
//  DIAG_SUBSYS_MUX1X              = 27,      /* Multiplexer */
//  DIAG_SUBSYS_DATA1X             = 28,      /* Data */
//  DIAG_SUBSYS_SRCH1X             = 29,      /* Searcher */
//  DIAG_SUBSYS_CALLP1X            = 30,      /* Call Processor */
//  DIAG_SUBSYS_APPS               = 31,      /* Applications */
//  DIAG_SUBSYS_SETTINGS           = 32,      /* Settings */
//  DIAG_SUBSYS_GSDI               = 33,      /* Generic SIM Driver Interface */
//  DIAG_SUBSYS_UIMDIAG            = DIAG_SUBSYS_GSDI,
//  DIAG_SUBSYS_TMC                = 34,      /* Task Main Controller */
//  DIAG_SUBSYS_USB                = 35,      /* Universal Serial Bus */
//  DIAG_SUBSYS_PM                 = 36,      /* Power Management */
//  DIAG_SUBSYS_DEBUG              = 37,
//  DIAG_SUBSYS_QTV                = 38,
//  DIAG_SUBSYS_CLKRGM             = 39,      /* Clock Regime */
//  DIAG_SUBSYS_DEVICES            = 40,
//  DIAG_SUBSYS_WLAN               = 41,      /* 802.11 Technology */
//  DIAG_SUBSYS_PS_DATA_LOGGING    = 42,      /* Data Path Logging */
//  DIAG_SUBSYS_PS                 = DIAG_SUBSYS_PS_DATA_LOGGING,
//  DIAG_SUBSYS_MFLO               = 43,      /* MediaFLO */
//  DIAG_SUBSYS_DTV                = 44,      /* Digital TV */
//  DIAG_SUBSYS_RRC                = 45,      /* WCDMA Radio Resource Control state */
//  DIAG_SUBSYS_PROF               = 46,      /* Miscellaneous Profiling Related */
//  DIAG_SUBSYS_TCXOMGR            = 47,
//  DIAG_SUBSYS_NV                 = 48,      /* Non Volatile Memory */
//  DIAG_SUBSYS_AUTOCONFIG         = 49,
//  DIAG_SUBSYS_PARAMS             = 50,      /* Parameters required for debugging subsystems */
//  DIAG_SUBSYS_MDDI               = 51,      /* Mobile Display Digital Interface */
//  DIAG_SUBSYS_DS_ATCOP           = 52,
//  DIAG_SUBSYS_L4LINUX            = 53,      /* L4/Linux */
//  DIAG_SUBSYS_MVS                = 54,      /* Multimode Voice Services */
//  DIAG_SUBSYS_CNV                = 55,      /* Compact NV */
//  DIAG_SUBSYS_APIONE_PROGRAM     = 56,      /* apiOne */
//  DIAG_SUBSYS_HIT                = 57,      /* Hardware Integration Test */
//  DIAG_SUBSYS_DRM                = 58,      /* Digital Rights Management */
//  DIAG_SUBSYS_DM                 = 59,      /* Device Management */
//  DIAG_SUBSYS_FC                 = 60,      /* Flow Controller */
//  DIAG_SUBSYS_MEMORY             = 61,      /* Malloc Manager */
//  DIAG_SUBSYS_FS_ALTERNATE       = 62,      /* Alternate File System */
//  DIAG_SUBSYS_REGRESSION         = 63,      /* Regression Test Commands */
//  DIAG_SUBSYS_SENSORS            = 64,      /* The sensors subsystem */
//  DIAG_SUBSYS_FLUTE              = 65,      /* FLUTE */
//  DIAG_SUBSYS_ANALOG             = 66,      /* Analog die subsystem */
//  DIAG_SUBSYS_APIONE_PROGRAM_MODEM = 67,    /* apiOne Program On Modem Processor */
//  DIAG_SUBSYS_LTE                = 68,      /* LTE */
//  DIAG_SUBSYS_BREW               = 69,      /* BREW */
//  DIAG_SUBSYS_PWRDB              = 70,      /* Power Debug Tool */
//  DIAG_SUBSYS_CHORD              = 71,      /* Chaos Coordinator */
//  DIAG_SUBSYS_SEC                = 72,      /* Security */
//  DIAG_SUBSYS_TIME               = 73,      /* Time Services */
//  DIAG_SUBSYS_Q6_CORE            = 74,		  /* Q6 core services */
//
//  DIAG_SUBSYS_LAST,
//
//  /* Subsystem IDs reserved for OEM use */
//  DIAG_SUBSYS_RESERVED_OEM_0     = 250,
//  DIAG_SUBSYS_RESERVED_OEM_1     = 251,
//  DIAG_SUBSYS_RESERVED_OEM_2     = 252,
//  DIAG_SUBSYS_RESERVED_OEM_3     = 253,
//  DIAG_SUBSYS_RESERVED_OEM_4     = 254,
//  DIAG_SUBSYS_LEGACY             = 255
//}
}
