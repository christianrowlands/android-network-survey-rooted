package com.craxiom.networksurveyplus.ui.settings;

import android.content.Context;
import android.content.RestrictionsManager;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.craxiom.networksurveyplus.Constants;
import com.craxiom.networksurveyplus.R;

import timber.log.Timber;

/**
 * A fragment for the preference screen to present users with a set of user preferences.
 *
 * @since 0.1.0
 */
public class PreferencesFragment extends PreferenceFragmentCompat
{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource(R.xml.network_survey_settings, rootKey);

        updateUiForMdmIfNecessary();
    }

    /**
     * If the app is under MDM control, update the user preferences UI to reflect those MDM provided values. If the app
     * is not under MDM control, then do nothing.
     */
    private void updateUiForMdmIfNecessary()
    {
        if (!isUnderMdmControl()) return;

        final RestrictionsManager restrictionsManager = (RestrictionsManager) requireContext().getSystemService(Context.RESTRICTIONS_SERVICE);
        if (restrictionsManager != null)
        {
            final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();

            final PreferenceScreen preferenceScreen = getPreferenceScreen();

            updateIntPreferenceForMdm(preferenceScreen, Constants.PROPERTY_LOG_ROLLOVER_SIZE_MB, mdmProperties);
        }
    }

    /**
     * @return True if this app is under MDM control (aka at least one value is set via the MDM server).
     */
    private boolean isUnderMdmControl()
    {
        final RestrictionsManager restrictionsManager = (RestrictionsManager) requireContext().getSystemService(Context.RESTRICTIONS_SERVICE);
        if (restrictionsManager != null)
        {
            final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();

            if (mdmProperties.getInt(Constants.PROPERTY_LOG_ROLLOVER_SIZE_MB, 0) != 0)
            {
                Timber.i("Network Survey+ is under MDM control");
                return true;
            }
        }

        return false;
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
