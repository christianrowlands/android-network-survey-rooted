package com.craxiom.networksurveyplus.ui.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.craxiom.networksurveyplus.R;

public class PreferencesFragment extends PreferenceFragmentCompat
{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource(R.xml.network_survey_settings, rootKey);
    }
}
