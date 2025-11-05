package com.craxiom.networksurveyplus.processor;

import android.location.Location;

import com.craxiom.networksurveyplus.GpsListener;
import com.craxiom.networksurveyplus.IPcapMessageListener;
import com.craxiom.networksurveyplus.IServiceStatusListener;
import com.craxiom.networksurveyplus.ServiceStatusMessage;
import com.craxiom.networksurveyplus.messages.PcapMessage;
import com.craxiom.networksurveyplus.parser.ShannonLteParser;
import com.craxiom.networksurveyplus.parser.ShannonMessageChunker;
import com.craxiom.networksurveyplus.util.ParserUtils;

import java.nio.ByteOrder;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import timber.log.Timber;

/**
 * Processes Shannon SDM frames and converts them to PCAP messages.
 */
public class ShannonMessageProcessor implements MessageProcessor
{

    private final Set<IPcapMessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final Set<IServiceStatusListener> serviceListeners = new CopyOnWriteArraySet<>();
    private final GpsListener gpsListener;
    private final ShannonMessageChunker messageChunker = new ShannonMessageChunker();
    private int recordsProcessed = 0;

    // Shannon group constants
    private static final int GROUP_CONTROL = 0x00;
    private static final int GROUP_COMMON = 0x01;
    private static final int GROUP_LTE = 0x02;
    private static final int GROUP_EDGE_GSM = 0x03;
    private static final int GROUP_HSPA_WCDMA = 0x04;
    private static final int GROUP_TRACE = 0x05;
    private static final int GROUP_ILM = 0x06;
    private static final int GROUP_TCP_IP = 0x07;

    public ShannonMessageProcessor(GpsListener gpsListener)
    {
        this.gpsListener = gpsListener;
    }

    @Override
    public void onFrame(byte[] frame)
    {
        try
        {
            // Extract Shannon header
            byte group = (byte) (frame[9] & 0x1F);  // Mask to get lower 5 bits
            byte command = frame[10];
            long timestamp = ParserUtils.getInteger(frame, 11, ByteOrder.LITTLE_ENDIAN) & 0xFFFFFFFFL;

            // Extract payload (skip header + delimiters)
            int payloadLength = frame.length - 16; // Total - (start + 14 header + end)
            byte[] payload = new byte[payloadLength];
            System.arraycopy(frame, 15, payload, 0, payloadLength);

            Timber.v("Shannon frame: group=0x%02x, command=0x%02x, timestamp=%d, payload=%d bytes",
                    group, command, timestamp, payloadLength);

            // Convert to PCAP message
            PcapMessage pcapMessage = convertToPcapMessage(group, command, payload, timestamp);

            if (pcapMessage != null)
            {
                // Notify listeners
                for (IPcapMessageListener listener : messageListeners)
                {
                    listener.onPcapMessage(pcapMessage);
                }

                recordsProcessed++;

                // Notify service status listeners every 100 records
                if (recordsProcessed % 100 == 0)
                {
                    notifyStatusListeners();
                }
            }
        } catch (Exception e)
        {
            Timber.e(e, "Error processing Shannon frame");
        }
    }

    private PcapMessage convertToPcapMessage(byte group, byte command, byte[] payload, long timestamp)
    {
        try
        {
            // Get GPS location for PCAP record
            Location location = (gpsListener != null) ? gpsListener.getLatestLocation() : null;

            // SIM ID (Shannon doesn't provide this in messages, use 0)
            int simId = 0;

            // Route based on group and command
            switch (group)
            {
                case GROUP_LTE:
                    return handleLteMessage(command, payload, simId, location);

                case GROUP_EDGE_GSM:
                    Timber.v("GSM messages not yet supported (group=0x%02x, command=0x%02x)", group, command);
                    return null;

                case GROUP_HSPA_WCDMA:
                    Timber.v("WCDMA messages not yet supported (group=0x%02x, command=0x%02x)", group, command);
                    return null;

                case GROUP_CONTROL:
                case GROUP_COMMON:
                case GROUP_TRACE:
                case GROUP_ILM:
                case GROUP_TCP_IP:
                    // These groups don't contain cellular messages
                    Timber.v("Non-cellular Shannon group: 0x%02x, command=0x%02x", group, command);
                    return null;

                default:
                    Timber.v("Unknown Shannon group: 0x%02x, command=0x%02x", group, command);
                    return null;
            }
        } catch (Exception e)
        {
            Timber.e(e, "Error converting Shannon message to PCAP");
            return null;
        }
    }

    /**
     * Handles LTE group messages (group 0x02).
     */
    private PcapMessage handleLteMessage(byte command, byte[] payload, int simId, Location location)
    {
        switch (command & 0xFF)
        {
            case 0x52:  // LTE RRC OTA (single-frame)
                Timber.v("Shannon LTE RRC OTA message, payload=%d bytes", payload.length);
                return ShannonLteParser.convertLteRrcOtaMessage(payload, simId, location);

            case 0x54:  // LTE RRC (multi-chunk)
                Timber.v("Shannon LTE RRC chunk, payload=%d bytes", payload.length);
                return handleMultiChunkRrc(payload, simId, location);

            case 0x5A:  // LTE NAS EMM
                Timber.v("Shannon LTE NAS EMM message, payload=%d bytes", payload.length);
                return ShannonLteParser.convertLteNasMessage(payload, true, simId, location);

            case 0x5F:  // LTE NAS ESM
                Timber.v("Shannon LTE NAS ESM message, payload=%d bytes", payload.length);
                return ShannonLteParser.convertLteNasMessage(payload, false, simId, location);

            case 0x50:  // LTE RRC Serving Cell Info
                Timber.v("Shannon LTE serving cell info (metadata tracking not yet implemented)");
                return null;

            case 0x51:  // LTE RRC Status
                Timber.v("Shannon LTE RRC status (metadata only)");
                return null;

            default:
                Timber.v("Unhandled LTE command: 0x%02x", command & 0xFF);
                return null;
        }
    }

    /**
     * Handles multi-chunk RRC messages (command 0x54).
     * Stores chunks and returns complete message when all received.
     */
    private PcapMessage handleMultiChunkRrc(byte[] payload, int simId, Location location)
    {
        // Add chunk to reassembly buffer
        byte[] completeMessage = messageChunker.addChunk(payload);

        if (completeMessage != null)
        {
            // All chunks received, parse as RRC OTA
            // completeMessage already has 4-byte RRC header from reassembly
            return ShannonLteParser.convertLteRrcOtaMessage(completeMessage, simId, location);
        }

        // Waiting for more chunks
        return null;
    }

    @Override
    public void registerMessageListener(IPcapMessageListener listener)
    {
        messageListeners.add(listener);
    }

    @Override
    public void unregisterMessageListener(IPcapMessageListener listener)
    {
        messageListeners.remove(listener);
    }

    @Override
    public void registerRecordsLoggedListener(IServiceStatusListener listener)
    {
        serviceListeners.add(listener);
    }

    @Override
    public void unregisterRecordsLoggedListener(IServiceStatusListener listener)
    {
        serviceListeners.remove(listener);
    }

    @Override
    public boolean isBeingUsed()
    {
        return !messageListeners.isEmpty() || !serviceListeners.isEmpty();
    }

    /**
     * Notify all the listeners of the updated status if the proper amount of time has passed since the last notification.
     */
    private void notifyStatusListeners()
    {
        ServiceStatusMessage recordLoggedMessage = new ServiceStatusMessage(ServiceStatusMessage.SERVICE_RECORD_LOGGED_MESSAGE, recordsProcessed);
        for (IServiceStatusListener listener : serviceListeners)
        {
            try
            {
                listener.onServiceStatusMessage(recordLoggedMessage);
            } catch (Exception e)
            {
                Timber.e(e, "Unable to notify a Status Listener because of an exception");
            }
        }
    }
}
