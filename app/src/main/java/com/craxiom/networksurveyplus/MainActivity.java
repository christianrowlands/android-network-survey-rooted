package com.craxiom.networksurveyplus;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity
{
    private static final int ACCESS_PERMISSION_REQUEST_ID = 1;
    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private QcdmService qcdmService;
    private QcdmServiceConnection qcdmServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Force Dark Mode
        setContentView(R.layout.activity_main);

        setupNavigation();

        qcdmServiceConnection = new QcdmServiceConnection();

        setupNotificationChannel();

        // TODO only copy one of these
        copyConfigFile(R.raw.diag);
        copyConfigFile(R.raw.full_diag);
        copyConfigFile(R.raw.rrc_diag);
        copyConfigFile(R.raw.rrc_filter_diag);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (missingAnyPermissions()) showPermissionRationaleAndRequestPermissions();

        startAndBindToQcdmService();
    }

    @Override
    protected void onPause()
    {
        if (qcdmService != null)
        {
            final Context applicationContext = getApplicationContext();

            // TODO qcdmService.onUiHidden();

            // TODO if (!qcdmService.isBeingUsed())
            {
                // We can safely shutdown the service since both logging and the connections are turned off
                final Intent networkSurveyServiceIntent = new Intent(applicationContext, QcdmService.class);
                stopService(networkSurveyServiceIntent);
            }

            applicationContext.unbindService(qcdmServiceConnection);
        }

        super.onPause();
    }

    /**
     * Check to see if we should show the rationale for any of the permissions.  If so, then display a dialog that
     * explains what permissions we need for this app to work properly.
     * <p>
     * If we should not show the rationale, then just request the permissions.
     */
    private void showPermissionRationaleAndRequestPermissions()
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE))
        {
            Timber.d("Showing the permissions rationale dialog");

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setCancelable(true);
            alertBuilder.setTitle(getString(R.string.permissions_rationale_title));
            alertBuilder.setMessage(getText(R.string.permissions_rationale));
            alertBuilder.setPositiveButton(android.R.string.yes, (dialog, which) -> requestPermissions());

            AlertDialog permissionsExplanationDialog = alertBuilder.create();
            permissionsExplanationDialog.show();
        } else
        {
            requestPermissions();
        }
    }

    /**
     * Request the permissions needed for this app if any of them have not yet been granted.  If all of the permissions
     * are already granted then don't request anything.
     */
    private void requestPermissions()
    {
        if (missingAnyPermissions())
        {
            ActivityCompat.requestPermissions(this, PERMISSIONS, ACCESS_PERMISSION_REQUEST_ID);
        }
    }

    /**
     * @return True if any of the permissions for this app have been denied.  False if all the permissions have been granted.
     */
    private boolean missingAnyPermissions()
    {
        for (String permission : PERMISSIONS)
        {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
            {
                Timber.i("Missing the permission: %s", permission);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks that the location provider is enabled and that the location permission has been granted.  If GPS location
     * is not enabled on this device, then the settings UI is opened so the user can enable it.
     */
    /*private void checkLocationProvider()
    {
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
        {
            Log.w(LOG_TAG, "Could not get the location manager.  Skipping checking the location provider");
            return;
        }

        final LocationProvider locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (locationProvider == null)
        {
            final String noGpsMessage = getString(R.string.no_gps_device);
            Log.w(LOG_TAG, noGpsMessage);
            Toast.makeText(getApplicationContext(), noGpsMessage, Toast.LENGTH_LONG).show();
        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            // gps exists, but isn't on
            final String turnOnGpsMessage = getString(R.string.turn_on_gps);
            Log.w(LOG_TAG, turnOnGpsMessage);
            Toast.makeText(getApplicationContext(), turnOnGpsMessage, Toast.LENGTH_LONG).show();

            promptEnableGps();
        }
    }*/

    /**
     * Ask the user if they want to enable GPS.  If they do, then open the Location settings.
     */
    /*private void promptEnableGps()
    {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.enable_gps_message))
                .setPositiveButton(getString(R.string.enable_gps_positive_button),
                        (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                )
                .setNegativeButton(getString(R.string.enable_gps_negative_button),
                        (dialog, which) -> {
                        }
                )
                .show();
    }*/

    /**
     * Setup the bottom navigation view for this app.
     */
    private void setupNavigation()
    {
        final BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    /**
     * Create the notification channel that this app will use to display notifications in the Android UI.
     */
    private void setupNotificationChannel()
    {
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
        {
            final NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID,
                    getText(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        } else
        {
            Timber.e("The Notification Manager could not be retrieved to add the Network Survey notification channel");
        }
    }

    /**
     * Copies the provide config file (by its raw resource ID) to the app's private storage area so
     * that it can be used for diag access.
     *
     * @param resourceId The raw resource ID.
     */
    private void copyConfigFile(@RawRes int resourceId)
    {
        final Resources resources = getResources();
        final String filename = resources.getResourceEntryName(resourceId);
        final File configFile = new File(getFilesDir(), filename);

        try (final InputStream configFileInputStream = resources.openRawResource(resourceId);
             final OutputStream configFileOutputStream = new FileOutputStream(configFile))
        {
            NetworkSurveyUtils.copyInputStreamToOutputStream(configFileInputStream, configFileOutputStream);
        } catch (FileNotFoundException e)
        {
            final String message = "The " + filename + " file was not found in the app's raw directory";
            Timber.e(e, message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (IOException e)
        {
            final String message = "Could not create the " + filename + " file";
            Timber.e(e, message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start the Network Survey Service (it won't start if it is already started), and then bind to the service.
     * <p>
     * Starting the service will cause the cellular records to be pulled from the Android system so they can be shown
     * in the UI, logged to a file, sent over a connection, or any combination of the three.
     * <p>
     * The Network survey service also handles getting GNSS information so that it can be used accordingly.
     */
    private void startAndBindToQcdmService()
    {
        // Start and bind to the survey service
        final Context applicationContext = getApplicationContext();
        final Intent startServiceIntent = new Intent(applicationContext, QcdmService.class);
        startService(startServiceIntent);

        final Intent serviceIntent = new Intent(applicationContext, QcdmService.class);
        final boolean bound = applicationContext.bindService(serviceIntent, qcdmServiceConnection, Context.BIND_ABOVE_CLIENT);
        Timber.i("QcdmService bound in the MainActivity: %s", bound);
    }

    /**
     * A {@link ServiceConnection} implementation for binding to the {@link QcdmService}.
     */
    private class QcdmServiceConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder iBinder)
        {
            Timber.i("%s service connected", name);
            final QcdmService.QcdmServiceBinder binder = (QcdmService.QcdmServiceBinder) iBinder;
            qcdmService = binder.getService();
            // TODO Update this
            /*qcdmService.onUiVisible(NetworkSurveyActivity.this);

            final boolean cellularLoggingEnabled = qcdmService.isCellularLoggingEnabled();
            if (turnOnCellularLoggingOnNextServiceConnection && !cellularLoggingEnabled)
            {
                toggleCellularLogging(true);
            } else
            {
                updateCellularLoggingButton(cellularLoggingEnabled);
            }

            final boolean wifiLoggingEnabled = qcdmService.isWifiLoggingEnabled();
            if (turnOnWifiLoggingOnNextServiceConnection && !wifiLoggingEnabled)
            {
                toggleWifiLogging(true);
            } else
            {
                updateWifiLoggingButton(wifiLoggingEnabled);
            }

            final boolean gnssLoggingEnabled = qcdmService.isGnssLoggingEnabled();
            if (turnOnGnssLoggingOnNextServiceConnection && !gnssLoggingEnabled)
            {
                toggleGnssLogging(true);
            } else
            {
                updateGnssLoggingButton(gnssLoggingEnabled);
            }

            turnOnCellularLoggingOnNextServiceConnection = false;
            turnOnWifiLoggingOnNextServiceConnection = false;
            turnOnGnssLoggingOnNextServiceConnection = false;*/
        }

        @Override
        public void onServiceDisconnected(final ComponentName name)
        {
            qcdmService = null;
            Timber.i("%s service disconnected", name);
        }
    }
}