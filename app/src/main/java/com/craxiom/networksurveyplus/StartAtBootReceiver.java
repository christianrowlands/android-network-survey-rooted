package com.craxiom.networksurveyplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.craxiom.networksurveyplus.util.PreferenceUtils;

import timber.log.Timber;

/**
 * Starts the Network Survey Service when Android is booted if the {@link Constants#PROPERTY_AUTO_START_PCAP_LOGGING}
 * property has been set by the MDM managed configuration or if the {@link Constants#PROPERTY_MQTT_START_ON_BOOT}
 * property has been set in the MDM managed configuration or the regular user settings.
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

        if (PreferenceUtils.getMqttStartOnBootPreference(context))
        {
            Timber.i("Auto starting the QCDM Service based on the user or MDM MQTT auto start preference");

            startNetworkSurveyService(context);
        } else if (PreferenceUtils.getAutoStartPreference(Constants.PROPERTY_AUTO_START_PCAP_LOGGING, false, context))
        {
            Timber.i("Auto starting the QCDM Service based on the auto start preference");

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
