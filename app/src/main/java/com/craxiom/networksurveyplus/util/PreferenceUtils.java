/*
 * Copyright (C) 2012-2018 Paul Watts (paulcwatts@gmail.com), Sean J. Barbeau (sjbarbeau@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.craxiom.networksurveyplus.util;

import android.content.Context;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import com.craxiom.networksurveyplus.Constants;

import timber.log.Timber;

/**
 * A class containing utility methods related to preferences.
 *
 * @since 0.6.0
 */
public class PreferenceUtils
{
    /**
     * Gets the auto start preference associated with the provide preference key.
     * <p>
     * First, this method tries to pull the MDM provided auto start value. If it is not set (either because the device
     * is not under MDM control, or if that specific value is not set by the MDM administrator) then the value is pulled
     * from the Android Shared Preferences (aka from the user settings). If it is not set there then the provided default
     * value is used.
     * <p>
     * The only exception to this sequence is that if the user has toggled the MDM override switch in user settings,
     * then the user preference value will be used instead of the MDM value.
     *
     * @param autoStartPreferenceKey The preference key to use when pulling the value from MDM and Shared Preferences.
     * @param defaultAutoStart       The default auto start value to fall back on if it could not be found.
     * @param context                The context to use when getting the Shared Preferences and Restriction Manager.
     * @return The auto start preference to use.
     */
    public static boolean getAutoStartPreference(String autoStartPreferenceKey, boolean defaultAutoStart, Context context)
    {
        final RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);

        final boolean mdmOverride = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PROPERTY_MDM_OVERRIDE_KEY, false);

        // First try to use the MDM provided value.
        if (restrictionsManager != null && !mdmOverride)
        {
            final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();

            if (mdmProperties.containsKey(autoStartPreferenceKey))
            {
                return mdmProperties.getBoolean(autoStartPreferenceKey);
            }
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Next, try to use the value from user preferences, with a default fallback
        return preferences.getBoolean(autoStartPreferenceKey, defaultAutoStart);
    }

    /**
     * Gets the auto start MQTT connection preference.
     * <p>
     * First, this method tries to pull the MDM provided auto start MQTT value. If it is not set (either because the device
     * is not under MDM control, or if that specific value is not set by the MDM administrator) then the value is pulled
     * from the Android Shared Preferences (aka from the user settings). If it is not set there then the provided default
     * value is used.
     * <p>
     * The only exception to this sequence is that if the user has toggled the MDM override switch in user settings,
     * then the user preference value will be used instead of the MDM value.
     *
     * @param context The context to use when getting the Shared Preferences and Restriction Manager.
     * @return True if the MQTT connection should be started when the phone is booted, false otherwise.
     */
    public static boolean getMqttStartOnBootPreference(Context context)
    {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean mdmOverride = sharedPreferences.getBoolean(Constants.PROPERTY_MDM_OVERRIDE_KEY, false);

        final RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle mdmProperties = null;
        if (restrictionsManager != null) mdmProperties = restrictionsManager.getApplicationRestrictions();

        if (!mdmOverride
                && mdmProperties != null
                && mdmProperties.containsKey(Constants.PROPERTY_MQTT_START_ON_BOOT))
        {
            Timber.i("Using the MDM MQTT auto start preference");
            return mdmProperties.getBoolean(Constants.PROPERTY_MQTT_START_ON_BOOT);
        } else
        {
            Timber.i("Using the user MQTT auto start preference");

            return sharedPreferences.getBoolean(Constants.PROPERTY_MQTT_START_ON_BOOT, false);
        }
    }
}
