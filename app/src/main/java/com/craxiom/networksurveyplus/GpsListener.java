package com.craxiom.networksurveyplus;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * A GPS Listener that is registered with the Android Location Service so that we are notified of Location updates.
 * <p>
 * This code was modeled after the WiGLE Wi-Fi App GPSListener:
 * https://github.com/wiglenet/wigle-wifi-wardriving/blob/master/wiglewifiwardriving/src/main/java/net/wigle/wigleandroid/listener/GPSListener.java
 *
 * @since 0.1.0
 */
public class GpsListener implements LocationListener
{
    private static final float MIN_DISTANCE_ACCURACY = 44f; // Probably need to make this configurable

    private Location latestLocation;
    private List<IServiceStatusListener> serviceMessageListeners = new ArrayList<>();

    @Override
    public void onLocationChanged(Location location)
    {
        updateLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    @Override
    public void onProviderEnabled(String provider)
    {
        Timber.i("Location Provider (%s) has been enabled", provider);
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        Timber.i("Location Provider (%s) has been disabled", provider);

        if (LocationManager.GPS_PROVIDER.equals(provider)) latestLocation = null;
    }

    /**
     * Adds a location update listener.
     *
     * @param listener The listener to add
     */
    public void registerLocationUpdatesListener(IServiceStatusListener listener)
    {
        serviceMessageListeners.add(listener);
    }

    /**
     * Removes a location update listeners.
     *
     * @param listener The listener to remove
     */
    public void unregisterLocationUpdatesListener(IServiceStatusListener listener)
    {
        serviceMessageListeners.remove(listener);
    }

    public Location getLatestLocation()
    {
        return latestLocation;
    }

    /**
     * Updates the cached location with the newly provided location.
     *
     * @param newLocation The newly provided location.
     */
    private void updateLocation(Location newLocation)
    {
        if (newLocation != null && newLocation.getAccuracy() <= MIN_DISTANCE_ACCURACY)
        {
            latestLocation = newLocation;
        } else
        {
            Timber.d("The accuracy of the last GPS location (%d) is less than the required minimum (%d)", newLocation.getAccuracy(), MIN_DISTANCE_ACCURACY);
            latestLocation = null;
        }

        ServiceStatusMessage locationMessage = new ServiceStatusMessage(ServiceStatusMessage.SERVICE_LOCATION_MESSAGE, latestLocation);
        serviceMessageListeners.forEach(listener -> listener.onServiceStatusMessage(locationMessage));
    }
}
