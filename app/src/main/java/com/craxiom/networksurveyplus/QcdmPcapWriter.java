package com.craxiom.networksurveyplus;

import android.content.Context;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

import com.craxiom.networksurveyplus.messages.PcapMessage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import timber.log.Timber;

/**
 * Writes the QCDM messages to a PCAP file.
 * <p>
 * The PCAP file format can be found here:  https://wiki.wireshark.org/Development/LibpcapFileFormat#File_Format
 * <p>
 * This class handles writing the pcap file global header, and then it also adds the appropriate layer 3 (IP),
 * layer 4 (UDP), and GSM_TAP headers to the front of each QCDM message.
 * <p>
 * Credit goes to Wireshark, QCSuper (https://github.com/P1sec/QCSuper/blob/master/protocol/gsmtap.py), and
 * Mobile Sentinel (https://github.com/RUB-SysSec/mobile_sentinel/blob/master/app/src/main/python/writers/pcapwriter.py)
 * for the information on how to construct the appropriate headers to drop the QCDM messages in a pcap file.
 *
 * @since 0.1.0
 */
public class QcdmPcapWriter implements IPcapMessageListener
{
    private static final String LOG_DIRECTORY_NAME = "NetworkSurveyPlusData";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
    private static final int BYTES_PER_MEGABYTE = 1_048_576;

    /**
     * The 24 byte PCAP global header.
     */
    private static final byte[] PCAP_FILE_GLOBAL_HEADER = {
            (byte) 0xd4, (byte) 0xc3, (byte) 0xb2, (byte) 0xa1, // PCAP magic number in little endian
            2, 0, 4, 0, // Major and minor file version (2 bytes each)
            0, 0, 0, 0, // GMT Offset (4 bytes)
            0, 0, 0, 0, // Timestamp Accuracy: (4 bytes)
            (byte) 0xff, (byte) 0xff, 0, 0,  // Snapshot (length)
            (byte) 0xc0, 0, 0, 0  // Link Layer Type (4 bytes): 192 is LINKTYPE_PPI
    };

    /**
     * The current rollover size. The default value is 5 MB; see {@link R.xml#network_survey_settings}
     */
    private int maxLogSizeBytes = 5 * BYTES_PER_MEGABYTE;

    /**
     * A lock to protect the block of code writing a pcap record to file. This is so the following
     * steps happen atomically:
     * <ol>
     *    <li>Check if file needs to be rolled over</li>
     *    <li>Write pcap record to file</li>
     *    <li>Send current record count to listeners</li>
     * </ol>
     */
    private final Object pcapWriteLock = new Object();

    private BufferedOutputStream outputStream;

    private File currentPcapFile;
    private int currentFileSizeBytes = 0;

    @Override
    public void onPcapMessage(PcapMessage pcapMessage)
    {
        try
        {
            final byte[] pcapRecord = pcapMessage.getPcapRecord();
            if (pcapRecord != null)
            {
                Timber.d("Writing a message to the pcap file");

                synchronized (pcapWriteLock)
                {
                    // Write the pcap record to file
                    outputStream.write(pcapRecord);
                    outputStream.flush();

                    if (isRolloverNeeded(pcapRecord.length)) createNewPcapFile();
                }
            }
        } catch (Exception e)
        {
            Timber.e(e, "Could not handle a QCDM message");
        }
    }

    /**
     * Creates a new pcap file and stores the result in {@link #currentPcapFile}.
     * The {@link #outputStream} is also updated, and closed if it was previously in use.
     * <p>
     * This method MUST be called before the {@link #onPcapMessage(PcapMessage)} method as it sets
     * up the output stream that the method writes the QCDM message to.
     */
    public void createNewPcapFile() throws IOException
    {
        synchronized (pcapWriteLock)
        {
            if (outputStream != null)
            {
                outputStream.close();
            }

            currentFileSizeBytes = 0;

            currentPcapFile = new File(createNewFilePath());
            currentPcapFile.getParentFile().mkdirs();

            outputStream = new BufferedOutputStream(new FileOutputStream(currentPcapFile));
            outputStream.write(PCAP_FILE_GLOBAL_HEADER);
            outputStream.flush();
        }
    }

    /**
     * Check if we need to roll over the pcap file
     *
     * @param pcapRecordLength The length of pcap record that is about to be added to the log in bytes.
     * @return True, if a new log file needs to be created; false otherwise
     */
    private boolean isRolloverNeeded(int pcapRecordLength)
    {
        // Check if we need to roll over the pcap file
        currentFileSizeBytes += pcapRecordLength;
        return currentFileSizeBytes >= maxLogSizeBytes;
    }

    /**
     * Closes the pcap file's output stream.
     */
    public void close()
    {
        try
        {
            if (outputStream != null) outputStream.close();
        } catch (Exception e)
        {
            Timber.e(e, "Could not close the pcap file output stream");
        }
    }

    /**
     * Update the max log size if the preference has changed.
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (Constants.PROPERTY_LOG_ROLLOVER_SIZE_MB.equals(key))
        {
            final String rolloverSizeStringMb = sharedPreferences.getString(key, String.valueOf(Constants.DEFAULT_LOG_ROLLOVER_SIZE_MB));
            try
            {
                Timber.d("Received a change event for a log rollover size preference change; new value=%s", rolloverSizeStringMb);
                final int newLogSizeMax = Integer.parseInt(rolloverSizeStringMb) * BYTES_PER_MEGABYTE;
                if (newLogSizeMax != maxLogSizeBytes) maxLogSizeBytes = newLogSizeMax;
            } catch (Exception e)
            {
                Timber.e(e, "Could not convert the max log size user preference (%s) to an int", rolloverSizeStringMb);
            }
        }
    }

    /**
     * Update the max log size if the preference has changed via MDM.
     */
    public void onMdmPreferenceChanged(Context context)
    {
        final RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
        if (restrictionsManager == null) return;

        final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();
        final int newRolloverSizeMb = mdmProperties.getInt(Constants.PROPERTY_LOG_ROLLOVER_SIZE_MB);
        if (newRolloverSizeMb != 0)
        {
            Timber.d("Received an MDM change event for a log rollover size preference change; new value=%s", newRolloverSizeMb);
            final int newLogSizeMax = newRolloverSizeMb * BYTES_PER_MEGABYTE;
            if (newLogSizeMax != maxLogSizeBytes) maxLogSizeBytes = newLogSizeMax;
        }
    }

    /**
     * @return The full path to the public directory where we store the pcap files.
     */
    private String createNewFilePath()
    {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/" + LOG_DIRECTORY_NAME + "/" +
                Constants.PCAP_FILE_NAME_PREFIX + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + ".pcap";
    }
}
