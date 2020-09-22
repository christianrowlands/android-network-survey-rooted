package com.craxiom.networksurveyplus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A collection of utilities for use throughout the app.
 *
 * @since 0.1.0
 */
@SuppressWarnings("WeakerAccess")
public final class NetworkSurveyUtils
{
    /**
     * Copies the provided input stream to the provided output stream.
     *
     * @throws IOException If the first byte cannot be read for any reason other than the end of the
     *                     file, if the input stream has been closed, or if some other I/O error occurs.
     */
    public static void copyInputStreamToOutputStream(InputStream in, OutputStream out) throws IOException
    {
        final byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1)
        {
            out.write(buffer, 0, read);
        }
    }

    /**
     * Get the name of a file without the file extension or period.
     *
     * @param fileName File name to work on
     * @return file name without extension
     */
    public static String getNameWithoutExtension(String fileName)
    {
        int i = fileName.lastIndexOf('.');

        if (i > 0 && i < fileName.length() - 1)
        {
            return fileName.substring(0, i);
        }
        return fileName;
    }

    /**
     * Extract the extension (with the period) from the given file name.
     *
     * @param fileName File name to process
     * @return file extension with the period, or null if no period or nothing after the period
     */
    public static String getExtension(String fileName)
    {
        String ext = null;
        int i = fileName.lastIndexOf('.');

        if (i > 0 && i < fileName.length() - 1)
        {
            ext = fileName.substring(i).toLowerCase();
        }
        return ext;
    }

    /**
     * Converts floating point values representing latitude and longitude to unsigned 3 integer digit
     * values followed by a 7 digit mantissa
     *
     * @param value Signed floating point value between -180.0000000 and + 180.0000000, inclusive
     * @return An unsigned 32-bit (native endian) value between 0 and 3600000000 (inclusive)
     */
    public static long doubleToFixed37(double value)
    {
        if (value < -180.0 || value >= 180.0000001)
            return 0;
        long scaledVale =  (long) ((value) * (double) 10000000);
        return (long) (scaledVale +  ((long) 180 * 10000000));
    }

    /**
     * Converts floating point values representing altitude and sensor data to unsigned 6 integer digit
     * values followed by 4 digit mantissa
     *
     * @param value Signed floating point value between -180000.0000 and + 180000.0000, inclusive
     * @return An unsigned 32-bit (native endian) value between 0 and 3600000000 (inclusive)
     */
    public static long doubleToFixed64(double value)
    {
        if (value <= -180000.0001 || value >= 180000.0001)
            return 0;
        long scaledVale =  (long) ((value) * (double) 10000);
        return (long) (scaledVale +  ((long) 180000 * 10000));
    }
}
