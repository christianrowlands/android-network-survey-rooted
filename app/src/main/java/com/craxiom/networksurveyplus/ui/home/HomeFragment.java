package com.craxiom.networksurveyplus.ui.home;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.craxiom.networksurveyplus.R;
import com.craxiom.networksurveyplus.databinding.FragmentHomeBinding;

import java.text.DecimalFormat;
import java.util.Locale;

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

        homeViewModel.getLocation().observe(getViewLifecycleOwner(), this::updateLocationTextView);
        homeViewModel.getRecordCount().observe(getViewLifecycleOwner(),
                recordCount -> binding.tvRecordCount.setText(String.format(Locale.US, "%d", recordCount)));

        return binding.getRoot();
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
            tvLocation.setTextColor(Color.WHITE);
            tvLocation.setScaleY(1f);
            tvLocation.setTextScaleX(1f);
        } else
        {
            tvLocation.setText(R.string.low_gps_confidence);
            tvLocation.setTextColor(Color.YELLOW);
            tvLocation.setScaleY(.5f);
            tvLocation.setTextScaleX(.5f);
        }
    }
}