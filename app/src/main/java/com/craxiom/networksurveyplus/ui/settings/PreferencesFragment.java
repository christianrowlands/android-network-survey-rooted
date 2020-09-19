package com.craxiom.networksurveyplus.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;

import com.craxiom.networksurveyplus.R;
import com.craxiom.networksurveyplus.databinding.FragmentPreferencesBinding;

/**
 * A fragment for the preference screen. It registers and unregisters the listener that is managed
 * by the {@link PreferencesViewModel}.
 */
public class PreferencesFragment extends PreferenceFragmentCompat
{
    private FragmentPreferencesBinding binding;
    private PreferencesViewModel preferencesViewModel;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource(R.xml.network_survey_settings, rootKey);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        binding = FragmentPreferencesBinding.inflate(getLayoutInflater());
        preferencesViewModel = new ViewModelProvider(requireActivity()).get(PreferencesViewModel.class);
        binding.setVm(preferencesViewModel);

        preferencesViewModel.getListener().observe(getViewLifecycleOwner(), listener ->
        {
            this.sharedPreferenceChangeListener = listener;
        });

        if (sharedPreferenceChangeListener != null)
        {
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }
}
