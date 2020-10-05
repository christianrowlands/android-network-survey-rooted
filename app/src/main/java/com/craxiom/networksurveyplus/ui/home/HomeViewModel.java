package com.craxiom.networksurveyplus.ui.home;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.craxiom.networksurveyplus.IServiceStatusListener;
import com.craxiom.networksurveyplus.ServiceStatusMessage;

import timber.log.Timber;

/**
 * View model for notifying the {@link HomeFragment} of any service message data updates, i.e.
 * location updates as well as record count updates.
 */
public class HomeViewModel extends ViewModel implements IServiceStatusListener
{
    private final MutableLiveData<Location> location = new MutableLiveData<>();
    private final MutableLiveData<Integer> recordCount = new MutableLiveData<>();

    public LiveData<Location> getLocation()
    {
        return location;
    }

    public LiveData<Integer> getRecordCount()
    {
        return recordCount;
    }

    @Override
    public void onServiceStatusMessage(ServiceStatusMessage serviceMessage)
    {
        switch (serviceMessage.what)
        {
            case ServiceStatusMessage.SERVICE_LOCATION_MESSAGE:
                location.postValue((Location) serviceMessage.data);
                break;

            case ServiceStatusMessage.SERVICE_RECORD_LOGGED_MESSAGE:
                recordCount.postValue((Integer) serviceMessage.data);
                break;

            default:
                Timber.w("Unrecognized service message type: %s", serviceMessage.what);
                break;
        }
    }
}