package com.craxiom.networksurveyplus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.craxiom.networksurveyplus.ui.home.HomeViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;
import java.util.function.Supplier;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity
{
    private static final int ACCESS_PERMISSION_REQUEST_ID = 1;
    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private QcdmService qcdmService;
    private QcdmServiceConnection qcdmServiceConnection;

    private MenuItem startStopPcapLoggingMenuItem;

    private HomeViewModel homeViewModel;

    private boolean turnOnLoggingOnNextServiceConnection = false;
    private boolean hasRequestedPermissions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Force Dark Mode
        setContentView(R.layout.activity_main);

        // Install the defaults specified in the XML preferences file, this is only done the first time the app is opened
        PreferenceManager.setDefaultValues(this, R.xml.network_survey_settings, false);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        turnOnLoggingOnNextServiceConnection = preferences.getBoolean(Constants.PROPERTY_AUTO_START_PCAP_LOGGING, false);

        setupNavigation();

        setupViewModels();

        qcdmServiceConnection = new QcdmServiceConnection();

        setupNotificationChannel();

        //copyConfigFile(R.raw.diag);
        //copyConfigFile(R.raw.full_diag);
        //copyConfigFile(R.raw.rrc_diag);
        //copyConfigFile(R.raw.rrc_filter_diag);
        copyConfigFile(R.raw.rrc_filter_diag_edit);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (missingAnyPermissions() && !hasRequestedPermissions)
        {
            showPermissionRationaleAndRequestPermissions();
        }

        startAndBindToQcdmService();
    }

    @Override
    protected void onPause()
    {
        if (qcdmService != null)
        {
            final Context applicationContext = getApplicationContext();

            if (!qcdmService.isBeingUsed())
            {
                // We can safely shutdown the service since both logging and the connections are turned off
                final Intent networkSurveyServiceIntent = new Intent(applicationContext, QcdmService.class);
                stopService(networkSurveyServiceIntent);
            }

            applicationContext.unbindService(qcdmServiceConnection);
        }

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        startStopPcapLoggingMenuItem = menu.findItem(R.id.action_start_stop_pcap_logging);

        if (qcdmService != null) updatePcapLoggingButton(qcdmService.isPcapLoggingEnabled());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_start_stop_pcap_logging)
        {
            if (qcdmService != null) togglePcapLogging(!qcdmService.isPcapLoggingEnabled());
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
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
            hasRequestedPermissions = true;
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
            Timber.w("Could not get the location manager.  Skipping checking the location provider");
            return;
        }

        final LocationProvider locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (locationProvider == null)
        {
            final String noGpsMessage = getString(R.string.no_gps_device);
            Timber.w(noGpsMessage);
            Toast.makeText(getApplicationContext(), noGpsMessage, Toast.LENGTH_LONG).show();
        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            // gps exists, but isn't on
            final String turnOnGpsMessage = getString(R.string.turn_on_gps);
            Timber.w(turnOnGpsMessage);
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
                R.id.navigation_home, R.id.navigation_settings, R.id.navigation_mqtt)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    /**
     * Set up the view models needed for communicating between this activity, the service, and the
     * fragments in the bottom navigation.
     */
    private void setupViewModels()
    {
        final ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        homeViewModel = viewModelProvider.get(HomeViewModel.class);
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
     * Starts or stops writing the pcap log file based on the specified parameter.
     *
     * @param enable True if logging should be enabled, false if it should be turned off.
     */
    private void togglePcapLogging(boolean enable)
    {
        new ToggleLoggingTask(() -> {
            if (qcdmService != null) return qcdmService.togglePcapLogging(enable);
            return null;
        }, enabled -> {
            if (enabled == null) return getString(R.string.pcap_logging_toggle_failed);
            updatePcapLoggingButton(enabled);
            return getString(enabled ? R.string.pcap_logging_start_toast : R.string.pcap_logging_stop_toast);
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Updates the Cellular logging button based on the specified logging state.
     *
     * @param enabled True if logging is currently enabled, false otherwise.
     */
    private void updatePcapLoggingButton(boolean enabled)
    {
        if (startStopPcapLoggingMenuItem == null) return;

        final String menuTitle = getString(enabled ? R.string.action_stop_pcap_logging : R.string.action_start_pcap_logging);
        startStopPcapLoggingMenuItem.setTitle(menuTitle);

        ColorStateList colorStateList = null;
        if (enabled)
        {
            colorStateList = ColorStateList.valueOf(getColor(R.color.carrotOrangeBrighter));
        }

        startStopPcapLoggingMenuItem.setIconTintList(colorStateList);
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
     * A task to move the action of starting or stopping logging off of the UI thread.
     */
    @SuppressLint("StaticFieldLeak")
    private class ToggleLoggingTask extends AsyncTask<Void, Void, Boolean>
    {
        private final Supplier<Boolean> toggleLoggingFunction;
        private final Function<Boolean, String> postExecuteFunction;

        private ToggleLoggingTask(Supplier<Boolean> toggleLoggingFunction, Function<Boolean, String> postExecuteFunction)
        {
            this.toggleLoggingFunction = toggleLoggingFunction;
            this.postExecuteFunction = postExecuteFunction;
        }

        @Override
        protected Boolean doInBackground(Void... nothing)
        {
            return toggleLoggingFunction.get();
        }

        @Override
        protected void onPostExecute(Boolean enabled)
        {
            if (enabled == null)
            {
                // An exception occurred or something went wrong, so don't do anything
                Toast.makeText(getApplicationContext(), "Error: Could not enable Logging", Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(getApplicationContext(), postExecuteFunction.apply(enabled), Toast.LENGTH_SHORT).show();
        }
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

            if (homeViewModel != null)
            {
                qcdmService.registerServiceStatusListener(homeViewModel);
            }

            final boolean loggingEnabled = qcdmService.isPcapLoggingEnabled();
            if (turnOnLoggingOnNextServiceConnection && !loggingEnabled)
            {
                togglePcapLogging(true);
            } else
            {
                updatePcapLoggingButton(loggingEnabled);
            }

            turnOnLoggingOnNextServiceConnection = false;
        }

        @Override
        public void onServiceDisconnected(final ComponentName name)
        {
            if (homeViewModel != null)
            {
                qcdmService.unregisterServiceStatusListener(homeViewModel);
            }

            qcdmService = null;
            Timber.i("%s service disconnected", name);
        }
    }
}