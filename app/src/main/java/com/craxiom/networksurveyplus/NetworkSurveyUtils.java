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
}
