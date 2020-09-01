package com.craxiom.networksurveyplus.messages;

/**
 * Constants for the GSMTAP protocol.
 * <p>
 * A lot of the constants were pulled from here:  https://osmocom.org/projects/libosmocore/repository/revisions/master/entry/include/osmocom/core/gsmtap.h
 * <p>
 * Also see the enum classes that represent some of the constants for the subtypes {@link UmtsRrcSubtypes},
 * {@link LteRrcSubtypes}, {@link LteNasSubtypes}.
 */
public final class GsmtapConstants
{
    private GsmtapConstants()
    {
    }

    public static final int GSMTAP_TYPE_UM = 0x01;
    public static final int GSMTAP_TYPE_ABIS = 0x02;
    public static final int GSMTAP_TYPE_UM_BURST = 0x03;       /* raw burst bits */
    public static final int GSMTAP_TYPE_SIM = 0x04;            /* ISO 7816 smart card interface */
    public static final int GSMTAP_TYPE_TETRA_I1 = 0x05;       /* tetra air interface */
    public static final int GSMTAP_TYPE_TETRA_I1_BURST = 0x06; /* tetra air interface */
    public static final int GSMTAP_TYPE_WMX_BURST = 0x07;      /* WiMAX burst */
    public static final int GSMTAP_TYPE_GB_LLC = 0x08;         /* GPRS Gb interface: LLC */
    public static final int GSMTAP_TYPE_GB_SNDCP = 0x09;       /* GPRS Gb interface: SNDCP */
    public static final int GSMTAP_TYPE_GMR1_UM = 0x0a;        /* GMR-1 L2 packets */
    public static final int GSMTAP_TYPE_UMTS_RLC_MAC = 0x0b;
    public static final int GSMTAP_TYPE_UMTS_RRC = 0x0c;
    public static final int GSMTAP_TYPE_LTE_RRC = 0x0d;        /* LTE interface */
    public static final int GSMTAP_TYPE_LTE_MAC = 0x0e;        /* LTE MAC interface */
    public static final int GSMTAP_TYPE_LTE_MAC_FRAMED = 0x0f; /* LTE MAC with context hdr */
    public static final int GSMTAP_TYPE_OSMOCORE_LOG = 0x10;   /* libosmocore logging */
    public static final int GSMTAP_TYPE_QC_DIAG = 0x11;        /* Qualcomm DIAG frame */
    public static final int GSMTAP_TYPE_LTE_NAS = 0x12;        /* LTE Non-Access Stratum */
    public static final int GSMTAP_TYPE_E1T1 = 0x13;

    /* sub-types for TYPE_UM_BURST */
    public static final int GSMTAP_BURST_UNKNOWN = 0x00;
    public static final int GSMTAP_BURST_FCCH = 0x01;
    public static final int GSMTAP_BURST_PARTIAL_SCH = 0x02;
    public static final int GSMTAP_BURST_SCH = 0x03;
    public static final int GSMTAP_BURST_CTS_SCH = 0x04;
    public static final int GSMTAP_BURST_COMPACT_SCH = 0x05;
    public static final int GSMTAP_BURST_NORMAL = 0x06;
    public static final int GSMTAP_BURST_DUMMY = 0x07;
    public static final int GSMTAP_BURST_ACCESS = 0x08;
    public static final int GSMTAP_BURST_NONE = 0x09;

    /* sub-types for TYPE_UM */
    public static final int GSMTAP_CHANNEL_UNKNOWN = 0x00;
    public static final int GSMTAP_CHANNEL_BCCH = 0x01;
    public static final int GSMTAP_CHANNEL_CCCH = 0x02;
    public static final int GSMTAP_CHANNEL_RACH = 0x03;
    public static final int GSMTAP_CHANNEL_AGCH = 0x04;
    public static final int GSMTAP_CHANNEL_PCH = 0x05;
    public static final int GSMTAP_CHANNEL_SDCCH = 0x06;
    public static final int GSMTAP_CHANNEL_SDCCH4 = 0x07;
    public static final int GSMTAP_CHANNEL_SDCCH8 = 0x08;
    public static final int GSMTAP_CHANNEL_FACCH_F = 0x09;      /* Actually, it's FACCH/F (signaling) */
    public static final int GSMTAP_CHANNEL_FACCH_H = 0x0a;      /* Actually, it's FACCH/H (signaling) */
    public static final int GSMTAP_CHANNEL_PACCH = 0x0b;
    public static final int GSMTAP_CHANNEL_CBCH52 = 0x0c;
    public static final int GSMTAP_CHANNEL_PDTCH = 0x0d;
    /**
     * Pulled this comment from https://osmocom.org/projects/libosmocore/repository/revisions/master/entry/include/osmocom/core/gsmtap.h
     * For legacy reasons we use a mis-spelled name. PDCH is really the physical channel, but we use it as PDTCH.
     */
    public static final int GSMTAP_CHANNEL_PDCH = GSMTAP_CHANNEL_PDTCH;
    public static final int GSMTAP_CHANNEL_PTCCH = 0x0e;
    public static final int GSMTAP_CHANNEL_CBCH51 = 0x0f;
    public static final int GSMTAP_CHANNEL_VOICE_F = 0x10;        /* voice codec payload (FR/EFR/AMR) */
    public static final int GSMTAP_CHANNEL_VOICE_H = 0x11;        /* voice codec payload (HR/AMR) */
    public static final int GSMTAP_CHANNEL_TCH_F = GSMTAP_CHANNEL_FACCH_F;        /* We used the wrong naming in 2008 when we were young */
    public static final int GSMTAP_CHANNEL_TCH_H = GSMTAP_CHANNEL_FACCH_H;        /* We used the wrong naming in 2008 when we were young */
}
