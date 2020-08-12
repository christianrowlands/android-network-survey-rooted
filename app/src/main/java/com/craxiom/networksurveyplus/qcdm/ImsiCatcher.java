package com.craxiom.networksurveyplus.qcdm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.Vector;

public class ImsiCatcher
{
    private final long startTime;
    private final long endTime;
    private final long id;
    private final int mcc;
    private final int mnc;
    private final int lac;
    private final int cid;
    private final double latitude;
    private final double longitude;
    private final boolean valid;
    private final double score;
    private final double a1;
    private final double a2;
    private final double a4;
    private final double a5;
    private final double k1;
    private final double k2;
    private final double c1;
    private final double c2;
    private final double c3;
    private final double c4;
    private final double c5;
    private final double t1;
    private final double t3;
    private final double t4;
    private final double r1;
    private final double r2;
    private final double f1;
    SQLiteDatabase db;
    Context context;

    public ImsiCatcher(long startTime, long endTime, long id, int mcc,
                       int mnc, int lac, int cid, double latitude, double longitude, boolean valid, double score,
                       double a1, double a2, double a4, double a5, double k1, double k2, double c1, double c2,
                       double c3, double c4, double c5, double t1, double t3, double t4, double r1, double r2,
                       double f1, Context context)
    {
        super();
        this.startTime = startTime;
        this.endTime = endTime;
        this.id = id;
        this.mcc = mcc;
        this.mnc = mnc;
        this.lac = lac;
        this.cid = cid;
        this.valid = valid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.score = score;
        this.a1 = a1;
        this.a2 = a2;
        this.a4 = a4;
        this.a5 = a5;
        this.k1 = k1;
        this.k2 = k2;
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
        this.c4 = c4;
        this.c5 = c5;
        this.t1 = t1;
        this.t3 = t3;
        this.t4 = t4;
        this.r1 = r1;
        this.r2 = r2;
        this.f1 = f1;
        MsdDatabaseManager.initializeInstance(new MsdSQLiteOpenHelper(context));
        db = MsdDatabaseManager.getInstance().openDatabase();
        this.context = context;
    }

    /**
     * Start time when the IMSI Catcher was detected
     *
     * @return
     */
    public long getStartTime()
    {
        return startTime;
    }

    /**
     * End time when the IMSI Catcher was detected
     *
     * @return
     */
    public long getEndTime()
    {
        return endTime;
    }

    /**
     * id column of the silent sms in table session_info, can be used to retrieve the IMSI Catcher again using get(long id)
     *
     * @return
     */
    public long getId()
    {
        return id;
    }

    /**
     * MCC when the IMSI Catcher was received
     *
     * @return
     */
    public int getMcc()
    {
        return mcc;
    }

    /**
     * MNC when the IMSI Catcher was received
     *
     * @return
     */
    public int getMnc()
    {
        return mnc;
    }

    public int getLac()
    {
        return lac;
    }

    public int getCid()
    {
        return cid;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public boolean isValid()
    {
        return valid;
    }

    /**
     * Score for the IMSI catcher
     *
     * @return
     */
    public double getScore()
    {
        return score;
    }

    public double getA1()
    {
        return a1;
    }

    public double getA2()
    {
        return a2;
    }

    public double getA4()
    {
        return a4;
    }

    public double getA5()
    {
        return a5;
    }

    public double getK1()
    {
        return k1;
    }

    public double getK2()
    {
        return k2;
    }

    public double getC1()
    {
        return c1;
    }

    public double getC2()
    {
        return c2;
    }

    public double getC3()
    {
        return c3;
    }

    public double getC4()
    {
        return c4;
    }

    public double getC5()
    {
        return c5;
    }

    public double getT1()
    {
        return t1;
    }

    public double getT3()
    {
        return t3;
    }

    public double getT4()
    {
        return t4;
    }

    public double getR1()
    {
        return r1;
    }

    public double getR2()
    {
        return r2;
    }

    public double getF1()
    {
        return f1;
    }

    @Override
    public String toString()
    {
        StringBuffer result = new StringBuffer("ImsiCatcher: ID=" + id);
        // TODO: Add more fields
        return result.toString();
    }

    public Vector<DumpFile> getFiles(SQLiteDatabase db)
    {
        return DumpFile.getFiles(db, DumpFile.TYPE_ENCRYPTED_QDMON, startTime, endTime, 0);
    }

    public String getFullCellID()
    {
        return mcc +
                "/" + mnc +
                "/" + lac +
                "/" + cid;
    }

    public String getLocation()
    {
        if (valid)
        {
            return latitude + " | " + longitude;
        } else
        {
            return "-";
        }
    }
}
