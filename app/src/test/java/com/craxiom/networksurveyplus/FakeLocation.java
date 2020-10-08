package com.craxiom.networksurveyplus;

/**
 * A fake location class to use because the android jar used in test has all the methods as stubs.
 *
 * @since 0.1.0
 */
public class FakeLocation extends android.location.Location
{
    private static final int HAS_ALTITUDE_MASK = 1;

    private double mLatitude = 0.0;
    private double mLongitude = 0.0;
    private double mAltitude = 0.0f;

    private int mFieldsMask = 0;

    public FakeLocation()
    {
        super("FakeLocationProvider");
    }

    @Override
    public double getLatitude()
    {
        return mLatitude;
    }

    @Override
    public void setLatitude(double mLatitude)
    {
        this.mLatitude = mLatitude;
    }

    @Override
    public double getLongitude()
    {
        return mLongitude;
    }

    @Override
    public void setLongitude(double mLongitude)
    {
        this.mLongitude = mLongitude;
    }

    @Override
    public double getAltitude()
    {
        return mAltitude;
    }

    @Override
    public void setAltitude(double altitude)
    {
        mAltitude = altitude;
        mFieldsMask |= HAS_ALTITUDE_MASK;
    }

    @Override
    public boolean hasAltitude()
    {
        return (mFieldsMask & HAS_ALTITUDE_MASK) != 0;
    }
}
