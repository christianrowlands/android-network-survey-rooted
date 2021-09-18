package com.craxiom.networksurveyplus.ui.settings;

import android.content.Context;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.craxiom.networksurveyplus.Constants;
import com.craxiom.networksurveyplus.R;
import com.craxiom.networksurveyplus.util.MdmUtils;

import timber.log.Timber;

/**
 * A fragment for the preference screen to present users with a set of user preferences.
 *
 * @since 0.1.0
 */
public class PreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener
{
    /**
     * The list of preferences that can be set in both the MDM app restrictions, and this settings UI.
     */
    private static final String[] PROPERTY_KEYS = {Constants.PROPERTY_AUTO_START_PCAP_LOGGING,
            Constants.PROPERTY_LOG_ROLLOVER_SIZE_MB,
            Constants.PROPERTY_MQTT_START_ON_BOOT};

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource(R.xml.network_survey_settings, rootKey);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        updateUiForMdmIfNecessary();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        switch (key)
        {
            case Constants.PROPERTY_MDM_OVERRIDE_KEY:
                final boolean mdmOverride = sharedPreferences.getBoolean(key, false);

                Timber.d("mdmOverride Preference Changed to %s", mdmOverride);

                if (mdmOverride)
                {
                    final PreferenceScreen preferenceScreen = getPreferenceScreen();
                    for (String preferenceKey : PROPERTY_KEYS)
                    {
                        final Preference preference = preferenceScreen.findPreference(preferenceKey);
                        if (preference != null) preference.setEnabled(true);
                    }
                } else
                {
                    updateUiForMdmIfNecessary();
                }
                break;
        }
    }

    @Override
    public void onDestroyView()
    {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroyView();
    }

    /**
     * If the app is under MDM control, update the user preferences UI to reflect those MDM provided values. If the app
     * is not under MDM control, then do nothing.
     *
     * @since 0.6.0
     */
    private void updateUiForMdmIfNecessary()
    {
        if (!MdmUtils.isUnderMdmControl(requireContext(), PROPERTY_KEYS)) return;

        final RestrictionsManager restrictionsManager = (RestrictionsManager) requireContext().getSystemService(Context.RESTRICTIONS_SERVICE);
        if (restrictionsManager == null) return;

        final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();
        if (mdmProperties == null) return;

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        updateIntPreferenceForMdm(preferenceScreen, Constants.PROPERTY_LOG_ROLLOVER_SIZE_MB, mdmProperties);
        updateBooleanPreferenceForMdm(preferenceScreen, mdmProperties, Constants.PROPERTY_AUTO_START_PCAP_LOGGING);
        updateBooleanPreferenceForMdm(preferenceScreen, mdmProperties, Constants.PROPERTY_MQTT_START_ON_BOOT);
    }

    /**
     * Updates a boolean preference with an MDM value, if it exists. The shared preferences are
     * also updated, so that values are retained when MDM control is off.
     *
     * @param preferenceScreen The preference screen
     * @param mdmProperties    The map of mdm provided properties.
     * @param preferenceKey    The preference key
     * @since 0.6.0
     */
    private void updateBooleanPreferenceForMdm(PreferenceScreen preferenceScreen, Bundle mdmProperties, String preferenceKey)
    {
        try
        {
            final SwitchPreferenceCompat preference = preferenceScreen.findPreference(preferenceKey);

            if (preference != null && mdmProperties.containsKey(preferenceKey))
            {
                final boolean mdmBooleanProperty = mdmProperties.getBoolean(preferenceKey);

                preference.setEnabled(false);
                preference.setChecked(mdmBooleanProperty);

                getPreferenceManager().getSharedPreferences()
                        .edit()
                        .putBoolean(preferenceKey, mdmBooleanProperty)
                        .apply();
            }
        } catch (Exception e)
        {
            Timber.wtf(e, "Could not find the bool preferences or update the UI component for %s", preferenceKey);
        }
    }

    /**
     * Updates the UI preference to reflect MDM control by disabling the UI preference component and pulling the
     * specified preference value from the app restrictions.
     *
     * @param preferenceScreen The preference screen that contains the preference to set.
     * @param preferenceKey    The key that corresponds to the preference of interest.
     */
    private void updateIntPreferenceForMdm(PreferenceScreen preferenceScreen, String preferenceKey, Bundle mdmProperties)
    {
        try
        {
            final int defaultValue = 0;
            final int mdmIntProperty = mdmProperties.getInt(preferenceKey, defaultValue);
            if (mdmIntProperty != defaultValue)
            {
                final EditTextPreference preference = preferenceScreen.findPreference(preferenceKey);
                //noinspection ConstantConditions
                preference.setEnabled(false);

                preference.setText(String.valueOf(mdmIntProperty));
            }
        } catch (Exception e)
        {
            Timber.wtf(e, "Could not find the int preference or update the UI component for %s", preferenceKey);
        }
    }
}
