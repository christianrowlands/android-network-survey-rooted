package com.craxiom.networksurveyplus.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.craxiom.networksurveyplus.R;
import com.craxiom.networksurveyplus.ServiceStatusMessage;
import com.craxiom.networksurveyplus.databinding.FragmentHomeBinding;

import java.text.DecimalFormat;
import java.util.Locale;

import timber.log.Timber;

/**
 * The main fragment that the user interacts with. It is used to display basic status information about the state of
 * this app.
 *
 * @since 0.1.0
 */
public class HomeFragment extends Fragment
{
    private FragmentHomeBinding binding;
    private final DecimalFormat decimalFormat = new DecimalFormat("###.#####");

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentHomeBinding.inflate(inflater);
        final HomeViewModel homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        binding.setVm(homeViewModel);

        initializeView();

        final LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        homeViewModel.getLocation().observe(viewLifecycleOwner, this::updateLocationTextView);
        homeViewModel.getRecordCount().observe(viewLifecycleOwner,
                recordCount -> binding.tvRecordCount.setText(String.format(Locale.US, "%d", recordCount)));
        homeViewModel.getProviderStatus().observe(viewLifecycleOwner, this::updateLocationProviderStatus);

        return binding.getRoot();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // In the edge case event where the user has just granted the location permission but has not restarted the app,
        // we need to update the UI to show the new location in this onResume method. There might be better approaches
        // instead of recalling the initialize view method each time the fragment is resumed.
        initializeView();
    }

    @Override
    public void onDestroyView()
    {
        final HomeViewModel homeViewModel = binding.getVm();
        final LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        homeViewModel.getLocation().removeObservers(viewLifecycleOwner);
        homeViewModel.getRecordCount().removeObservers(viewLifecycleOwner);
        homeViewModel.getProviderStatus().removeObservers(viewLifecycleOwner);

        super.onDestroyView();
    }

    /**
     * Updates the view to have some basic status information such as the current state of the location.
     */
    private void initializeView()
    {
        final Integer recordCount = binding.getVm().getRecordCount().getValue();
        if (recordCount != null)
        {
            binding.tvRecordCount.setText(String.format(Locale.US, "%d", recordCount));
        }

        final TextView tvLocation = binding.tvLocation;

        final String displayText;
        final int textColor;

        if (!hasLocationPermission())
        {
            tvLocation.setText(getString(R.string.missing_location_permission));
            tvLocation.setTextColor(getResources().getColor(R.color.connectionStatusDisconnected, null));
            tvLocation.setScaleY(.7f);
            tvLocation.setTextScaleX(.7f);
            return;
        }

        final Location location = binding.getVm().getLocation().getValue();
        if (location != null)
        {
            updateLocationTextView(location);
            return;
        }

        final LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
        {
            Timber.wtf("Could not get the location manager.");
            displayText = getString(R.string.no_gps_device);
            textColor = R.color.connectionStatusDisconnected;
            tvLocation.setScaleY(.7f);
            tvLocation.setTextScaleX(.7f);
        } else
        {
            final LocationProvider locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
            if (locationProvider == null)
            {
                displayText = getString(R.string.no_gps_device);
                Timber.w(displayText);
                textColor = R.color.connectionStatusConnecting;
                tvLocation.setScaleY(.7f);
                tvLocation.setTextScaleX(.7f);
            } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            {
                // gps exists, but isn't on
                displayText = getString(R.string.turn_on_gps);
                Timber.w(displayText);
                textColor = R.color.connectionStatusConnecting;
            } else
            {
                displayText = getString(R.string.searching_for_location);
                textColor = R.color.connectionStatusConnecting;
            }
        }

        tvLocation.setText(displayText);
        tvLocation.setTextColor(getResources().getColor(textColor, null));
    }

    /**
     * @return True if the {@link Manifest.permission#ACCESS_FINE_LOCATION} permission has been granted.  False otherwise.
     */
    private boolean hasLocationPermission()
    {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Timber.w("The ACCESS_FINE_LOCATION permission has not been granted");
            return false;
        }

        return true;
    }

    /**
     * Updates the location text view with the latest latitude and longitude, or if the latest location is below the
     * accuracy threshold then the text view is updated to notify the user of such.
     *
     * @param latestLocation The latest location if available, or null if the accuracy is not good enough.
     */
    private void updateLocationTextView(Location latestLocation)
    {
        TextView tvLocation = binding.tvLocation;
        if (latestLocation != null)
        {
            final String latLonString = decimalFormat.format(latestLocation.getLatitude()) + ", " +
                    decimalFormat.format(latestLocation.getLongitude());
            tvLocation.setText(latLonString);
            tvLocation.setTextColor(getResources().getColor(R.color.illiniTextColorPrimary, null));
            tvLocation.setScaleY(1f);
            tvLocation.setTextScaleX(1f);
        } else
        {
            tvLocation.setText(R.string.low_gps_confidence);
            tvLocation.setTextColor(Color.YELLOW);
            tvLocation.setScaleY(.7f);
            tvLocation.setTextScaleX(.7f);
        }
    }

    /**
     * Updates the location UI based on the provided location provider status.
     *
     * @param status The new status of the location provider.
     */
    private void updateLocationProviderStatus(ServiceStatusMessage.LocationProviderStatus status)
    {
        final TextView tvLocation = binding.tvLocation;

        switch (status)
        {
            case GPS_PROVIDER_ENABLED:
                tvLocation.setText(R.string.searching_for_location);
                tvLocation.setTextColor(getResources().getColor(R.color.connectionStatusConnecting, null));
                break;

            case GPS_PROVIDER_DISABLED:
                tvLocation.setText(R.string.turn_on_gps);
                tvLocation.setTextColor(getResources().getColor(R.color.connectionStatusConnecting, null));
                break;
        }
    }
}