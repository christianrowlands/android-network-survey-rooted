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

import com.craxiom.networksurveyplus.databinding.FragmentHomeBinding;

import java.text.DecimalFormat;

import timber.log.Timber;

public class HomeFragment extends Fragment
{
    private static final String LOG_TAG = HomeFragment.class.getSimpleName();

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private DecimalFormat decimalFormat = new DecimalFormat("###.####");

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentHomeBinding.inflate(inflater);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        binding.setVm(homeViewModel);

        homeViewModel.getLocation().observe(getViewLifecycleOwner(), this::updateLocationTextView);
        homeViewModel.getRecordCount().observe(getViewLifecycleOwner(), recordCount -> {
            Timber.w("Got record count update");
            binding.tvRecordCount.setText(recordCount);
        });

        return binding.getRoot();

        /*homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        final Button startButton = root.findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> startDiagStuff());

        return root;*/
    }

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
            tvLocation.setText("Low GPS Confidence");
            tvLocation.setTextColor(Color.YELLOW);
            tvLocation.setScaleY(.5f);
            tvLocation.setTextScaleX(.5f);
        }
    }
}