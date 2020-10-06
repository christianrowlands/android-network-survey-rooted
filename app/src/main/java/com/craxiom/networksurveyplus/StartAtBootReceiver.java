package com.craxiom.networksurveyplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionsManager;
import android.os.Bundle;

import timber.log.Timber;

/**
 * Starts the Network Survey Service when Android is booted if the {@link Constants#PROPERTY_AUTO_START_PCAP_LOGGING}
 * property has been set by the MDM managed configuration.
 *
 * @since 0.1.0
 */
public class StartAtBootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (null == intent) return;

        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        Timber.d("Received the boot completed broadcast message in the Network Survey+ broadcast receiver");

        final RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        if (restrictionsManager == null) return;

        final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();
        if (mdmProperties.getBoolean(Constants.PROPERTY_AUTO_START_PCAP_LOGGING, false))
        {
            Timber.i("Auto starting the QCDM Service based on the MDM auto start preference");

            startNetworkSurveyService(context);
        }
    }

    /**
     * Kick off the {@link QcdmService} using an intent. The {@link Constants#EXTRA_STARTED_AT_BOOT}
     * flag is used so that the {@link QcdmService} can handle being started at boot instead of when
     * the app is opened by the user.
     */
    private void startNetworkSurveyService(Context context)
    {
        final Context applicationContext = context.getApplicationContext();
        final Intent startServiceIntent = new Intent(applicationContext, QcdmService.class);
        startServiceIntent.putExtra(Constants.EXTRA_STARTED_AT_BOOT, true);
        context.startForegroundService(startServiceIntent);
    }
}
