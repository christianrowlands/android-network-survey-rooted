package com.craxiom.networksurveyplus.ui.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.craxiom.networksurveyplus.R;

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
    }
}
