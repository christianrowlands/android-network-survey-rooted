package com.craxiom.networksurveyplus.messages;

/**
 * The GSMTAP mapping for the GSM subtypes. The ordinal value of these enums map to the value that is used in the
 * GSMTAP header, so don't change the order of the enum values, and don't insert any new values in the middle.
 * <p>
 * These values are pulled from Osmocom and scat.
 * https://github.com/osmocom/libosmocore/blob/master/include/osmocom/core/gsmtap.h
 *
 * @since 0.4.0
 */
public enum GsmSubtypes
{
    GSMTAP_CHANNEL_UNKNOWN, // 0x00
    GSMTAP_CHANNEL_BCCH, // 0x01
    GSMTAP_CHANNEL_CCCH, // 0x02
    GSMTAP_CHANNEL_RACH, // 0x03
    GSMTAP_CHANNEL_AGCH, // 0x04
    GSMTAP_CHANNEL_PCH, // 0x05
    GSMTAP_CHANNEL_SDCCH, // 0x06
    GSMTAP_CHANNEL_SDCCH4, // 0x07
    GSMTAP_CHANNEL_SDCCH8, // 0x08
    GSMTAP_CHANNEL_TCH_F, // 0x09
    GSMTAP_CHANNEL_TCH_H, // 0x0a = 10
    GSMTAP_CHANNEL_PACCH, // 0x0b = 11
    GSMTAP_CHANNEL_CBCH52, // 0x0c = 12
    GSMTAP_CHANNEL_PDCH, // 0x0d = 13
    GSMTAP_CHANNEL_PTCCH, // 0x0e = 14
    GSMTAP_CHANNEL_CBCH51, // 0x0f = 15
    GSMTAP_CHANNEL_VOICE_F, // 0x10 = 16 /* voice codec payload (FR/EFR/AMR) */
    GSMTAP_CHANNEL_VOICE_H, // 0x11	= 17 /* voice codec payload (HR/AMR) */
}
