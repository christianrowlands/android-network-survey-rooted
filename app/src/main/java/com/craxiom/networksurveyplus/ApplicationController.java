package com.craxiom.networksurveyplus;

import android.app.Application;

import timber.log.Timber;

/**
 * The main application class for this app. Responsible for handling any initialization that needs
 * to occur as early in the Apps lifecycle as possible.
 *
 * @since 0.1.0
 */
public class ApplicationController extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        // If this is a debug apk, then we enable logging. If it is a release apk we don't want to
        // output any logs.
        if (BuildConfig.DEBUG)
        {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
