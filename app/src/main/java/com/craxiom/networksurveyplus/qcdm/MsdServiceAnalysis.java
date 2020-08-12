package com.craxiom.networksurveyplus.qcdm;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class MsdServiceAnalysis
{

    private static final String TAG = "MsdServiceAnalysis";

    private static int getLast(SQLiteDatabase db, String tableName)
    {
        try
        {
            Cursor c = db.rawQuery("SELECT * FROM " + tableName, null);
            int result = c.getCount();
            c.close();
            return result;
        } catch (SQLException e)
        {
            throw new IllegalStateException("SQLException in getLast(" + tableName + ",): ", e);
        }
    }

    public static int runCatcherAnalysis(Context context, SQLiteDatabase db) throws Exception
    {
        int before, after;

        before = getLast(db, "catcher");
        MsdSQLiteOpenHelper.readSQLAsset(context, db, "catcher_analysis.sql", false);
        after = getLast(db, "catcher");

        if (after != before)
        {
            int numResults = after - before;

            // New analysis results
            Log.i(TAG, "CatcherAnalysis: " + numResults + " new catcher results");

            return after - before;
        }
        return 0;
    }

    public static int runEventAnalysis(Context context, SQLiteDatabase db) throws Exception
    {
        int before, after;

        String[] event_cols = {"sum(CASE WHEN event_type > 0 THEN 1 ELSE 0 END)"};

        before = getLast(db, "events");
        MsdSQLiteOpenHelper.readSQLAsset(context, db, "event_analysis.sql", false);
        after = getLast(db, "events");

        if (after > before)
        {
            Cursor c = db.query
                    ("events",
                            event_cols,
                            "id > ? AND id <= ?",
                            new String[]{String.valueOf(before), String.valueOf(after)},
                            null, null, null);

            if (!c.moveToFirst())
            {
                throw new IllegalStateException("Invalid event result");
            }
            int numResults = c.getInt(0);
            c.close();
            Log.i(TAG, "EventAnalysis: " + numResults + " new result(s)");

            if (numResults > 0)
            {
                return numResults;
            }
        }
        return 0;
    }
}
