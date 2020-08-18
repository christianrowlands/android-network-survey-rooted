package com.craxiom.networksurveyplus;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.craxiom.networksurveyplus.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity
{
    private static final String LOG_TAG = HomeFragment.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        // TODO only copy one of these
        copyConfigFile(R.raw.diag);
        copyConfigFile(R.raw.full_diag);
        copyConfigFile(R.raw.rrc_diag);
        copyConfigFile(R.raw.rrc_filter_diag);
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
            Log.e(LOG_TAG, message, e);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (IOException e)
        {
            final String message = "Could not create the " + filename + " file";
            Log.e(LOG_TAG, message, e);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
}