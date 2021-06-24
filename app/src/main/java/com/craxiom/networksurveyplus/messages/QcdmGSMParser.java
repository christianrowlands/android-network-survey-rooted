package com.craxiom.networksurveyplus.messages;

import android.location.Location;

import timber.log.Timber;

public class QcdmGSMParser {

    public QcdmGSMParser() {
    }

    public static void convertGSMRRSignaling(QcdmMessage qcdmMessage, Location latestLocation) {
        Timber.v("Handling a GSM RR Signaling message");

        //return convertGSMRRSignaling(qcdmMessage, location, false, qcdmMessage.getSimId());
    }

    public static void convertGSMPowerScan(QcdmMessage qcdmMessage, Location latestLocation) {
        Timber.v("Handling a GSM Power Scan message");
        //return convertGSMPowerScan(qcdmMessage, location, false, qcdmMessage.getSimId());
    }
}

