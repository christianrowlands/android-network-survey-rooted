package com.craxiom.networksurveyplus.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.craxiom.networksurveyplus.Constants;
import com.craxiom.networksurveyplus.IServiceMessageListener;
import com.craxiom.networksurveyplus.ServiceMessage;
import com.craxiom.networksurveyplus.databinding.FragmentHomeBinding;

import java.text.DecimalFormat;

import timber.log.Timber;

public class HomeFragment extends Fragment implements LocationListener, IServiceMessageListener
{
    private static final String LOG_TAG = HomeFragment.class.getSimpleName();

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private Context context;
    private DecimalFormat decimalFormat = new DecimalFormat("###.####");

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        final View view = binding.getRoot();

        binding.tvRecordCount.setText(0);

        return view;

        /*homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        final Button startButton = root.findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> startDiagStuff());

        return root;*/
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);

        this.context = context;
        initializeLocationListener();
    }

    /**
     * Registers with the Android {@link LocationManager} for location updates.
     */
    private void initializeLocationListener()
    {
        if (context == null)
        {
            Timber.w("Cannot initialize location listener for HomeFragment without access to main activity");
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Timber.w("ACCESS_FINE_LOCATION Permission not granted for the QcdmService");
            return;
        }

        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null)
        {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            final int refreshRate = preferences.getInt(Constants.PROPERTY_LOCATION_REFRESH_RATE_MS, 5000);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, refreshRate, 0f, this);
        } else
        {
            Timber.e("The location manager was null when trying to request location updates for the QcdmService");
        }
    }

    private void updateLocationTextView(Location latestLocation)
    {
        TextView tvLocation = binding.tvLocation;
        final String latLonString = decimalFormat.format(latestLocation.getLatitude()) + ", " +
                decimalFormat.format(latestLocation.getLongitude());
        tvLocation.setText(latLonString);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        updateLocationTextView(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    @Override
    public void onProviderEnabled(String provider)
    {

    }

    @Override
    public void onProviderDisabled(String provider)
    {

    }

    @Override
    public void onServiceMessage(ServiceMessage serviceMessage)
    {
        switch (serviceMessage.what)
        {
            case Constants.SERVICE_LOCATION_MESSAGE:
                updateLocationTextView((Location) serviceMessage.data);
                break;
            case Constants.SERVICE_RECORD_LOGGED_MESSAGE:
                binding.tvRecordCount.setText((int) serviceMessage.data);
                break;
            default:
                Timber.w("Unrecognized service message type: %s", serviceMessage.what);
        }
    }
}