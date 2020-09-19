package com.craxiom.networksurveyplus.ui.settings;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * A view model used to register and notify classes of any preference changes.
 */
public class PreferencesViewModel extends ViewModel
{
    private final MutableLiveData<SharedPreferences.OnSharedPreferenceChangeListener> listener = new MutableLiveData<>();

    public LiveData<SharedPreferences.OnSharedPreferenceChangeListener> getListener()
    {
        return listener;
    }

    public void setListener(SharedPreferences.OnSharedPreferenceChangeListener listener)
    {
        this.listener.setValue(listener);
    }
}
