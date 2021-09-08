package com.craxiom.networksurveyplus;

import com.craxiom.networksurveyplus.messages.DiagCommand;
import com.craxiom.networksurveyplus.messages.DiagRevealerMessage;
import com.craxiom.networksurveyplus.messages.PcapMessage;
import com.craxiom.networksurveyplus.messages.QcdmConstants;
import com.craxiom.networksurveyplus.messages.QcdmMessage;
import com.craxiom.networksurveyplus.parser.QcdmGsmParser;
import com.craxiom.networksurveyplus.parser.QcdmLteParser;
import com.craxiom.networksurveyplus.parser.QcdmUmtsParser;
import com.craxiom.networksurveyplus.parser.QcdmWcdmaParser;
import com.craxiom.networksurveyplus.util.ParserUtils;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import timber.log.Timber;

/**
 * Responsible for consuming {@link DiagRevealerMessage} objects, cleaning them up by removing any headers and footers,
 * converting to a pcap record, and then notifying any listeners of the new message.
 *
 * @since 0.1.0
 */
public class QcdmMessageProcessor
{
    private final Set<IPcapMessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final Set<IServiceStatusListener> serviceMessageListeners = new CopyOnWriteArraySet<>();
    private final GpsListener gpsListener;

    /**
     * Overall record count since processing started, across all log files and MQTT connections that have been generated.
     */
    private int recordsProcessed = 0;

    /**
     * Constructs a new QCDM Message Processor.
     *
     * @param gpsListener The Location Listener to pull the location from for setting on each pcap record.
     */
    public QcdmMessageProcessor(GpsListener gpsListener)
    {
        this.gpsListener = gpsListener;
    }

    /**
     * Adds a status listener.
     *
     * @param listener The listener to add.
     * @since 0.5.0
     */
    public void registerRecordsLoggedListener(IServiceStatusListener listener)
    {
        serviceMessageListeners.add(listener);
    }

    /**
     * Removes a status listener.
     *
     * @param listener The listener to remove.
     * @since 0.5.0
     */
    public void unregisterRecordsLoggedListener(IServiceStatusListener listener)
    {
        serviceMessageListeners.remove(listener);
    }

    /**
     * @return True if either the UI or a listener needs this survey record processor.  False if the UI is hidden and
     * there are not any listeners.
     */
    synchronized boolean isBeingUsed()
    {
        return !messageListeners.isEmpty();
    }

    /**
     * Adds a listener for when this class has finished processing a QCDM message into a PCAP message.
     *
     * @param qcdmMessageListener The listener to add.
     */
    void registerQcdmMessageListener(IPcapMessageListener qcdmMessageListener)
    {
        messageListeners.add(qcdmMessageListener);
    }

    /**
     * Removes a listener so that it will no longer be notified of new PCAP messages.
     *
     * @param qcdmMessageListener The listener to remove.
     */
    void unregisterQcdmMessageListener(IPcapMessageListener qcdmMessageListener)
    {
        messageListeners.remove(qcdmMessageListener);
    }

    /**
     * Called when a new diag revealer message is ready.
     *
     * @param diagRevealerMessage the message received from the diag revealer.
     */
    void onDiagRevealerMessage(DiagRevealerMessage diagRevealerMessage)
    {
        Timber.v("Incoming Diag Revealer Message: %s", diagRevealerMessage);

        // No reason to process the message if we don't have any listeners
        if (!messageListeners.isEmpty())
        {
            ParserUtils.processDiagRevealerMessage(diagRevealerMessage, this::convertQcdmMessage);
        }
    }

    /**
     * Converts the provided {@link QcdmMessage} to a pcap record.
     *
     * @param qcdmMessage The QCDM message to convert to a pcap record so that it can be written to a file or streamed.
     */
    private void convertQcdmMessage(QcdmMessage qcdmMessage)
    {
        try
        {
            final int logType = qcdmMessage.getLogType();

            if (qcdmMessage.getOpCode() == DiagCommand.DIAG_LOG_F)
            {
                Timber.d("QCDM Processor: %s", qcdmMessage);

                PcapMessage pcapMessage = null;

                switch (logType)
                {
                    case QcdmConstants.LOG_LTE_RRC_OTA_MSG_LOG_C:
                        pcapMessage = QcdmLteParser.convertLteRrcOtaMessage(qcdmMessage, gpsListener.getLatestLocation());
                        break;

                    case QcdmConstants.LOG_LTE_NAS_EMM_OTA_IN_MSG:
                    case QcdmConstants.LOG_LTE_NAS_EMM_OTA_OUT_MSG:
                    case QcdmConstants.LOG_LTE_NAS_ESM_OTA_IN_MSG:
                    case QcdmConstants.LOG_LTE_NAS_ESM_OTA_OUT_MSG:
                    case QcdmConstants.LOG_LTE_NAS_EMM_SEC_OTA_IN_MSG:
                    case QcdmConstants.LOG_LTE_NAS_EMM_SEC_OTA_OUT_MSG:
                    case QcdmConstants.LOG_LTE_NAS_ESM_SEC_OTA_IN_MSG:
                    case QcdmConstants.LOG_LTE_NAS_ESM_SEC_OTA_OUT_MSG:
                        pcapMessage = QcdmLteParser.convertLteNasMessage(qcdmMessage, gpsListener.getLatestLocation());
                        break;

                    /*case QcdmConstants.WCDMA_SEARCH_CELL_RESELECTION_RANK:
                        Timber.i("WCDMA_SEARCH_CELL_RESELECTION_RANK");
                        break;
                    case QcdmConstants.WCDMA_RRC_STATES:
                        Timber.i("WCDMA_RRC_STATES");
                        break;
                    case QcdmConstants.WCDMA_CELL_ID:
                        Timber.i("WCDMA_CELL_ID");
                        break;
                    case QcdmConstants.WCDMA_SIB:
                        Timber.i("WCDMA_SIB");
                        break;*/
                    case QcdmConstants.WCDMA_SIGNALING_MESSAGES:
                        pcapMessage = QcdmWcdmaParser.convertWcdmaSignalingMessage(qcdmMessage, gpsListener.getLatestLocation());
                        break;

                    case QcdmConstants.UMTS_NAS_OTA:
                        pcapMessage = QcdmUmtsParser.convertUmtsNasOta(qcdmMessage, gpsListener.getLatestLocation());
                        break;

                    case QcdmConstants.UMTS_NAS_OTA_DSDS:
                        pcapMessage = QcdmUmtsParser.convertUmtsNasOtaDsds(qcdmMessage, gpsListener.getLatestLocation());
                        break;

                    case QcdmConstants.GSM_RR_SIGNALING_MESSAGES:
                        pcapMessage = QcdmGsmParser.convertGsmSignalingMessage(qcdmMessage, gpsListener.getLatestLocation());
                        break;

                    /*case QcdmConstants.GSM_RR_CELL_INFORMATION_C:
                        // TODO delete me once I find a record for this
                        Timber.i("GSM RR Cell Information: %s", qcdmMessage);
                        break;*/

                    default:
                        Timber.v("Unhandled QCDM log type for the QCDM Message processor %s", logType);
                }

                if (pcapMessage != null)
                {
                    Timber.d("Successfully processed a QCDM message into a PCAP record");

                    // Notify listeners of record count
                    recordsProcessed++;
                    notifyStatusListeners();
                    notifyPcapMessageListeners(pcapMessage);
                }
            }
        } catch (Exception e)
        {
            Timber.e(e, "Could not handle a QCDM message");
        }
    }

    /**
     * Notify all the listeners of the updated status if the proper amount of time has passed since the last notification.
     */
    private void notifyStatusListeners()
    {
        ServiceStatusMessage recordLoggedMessage = new ServiceStatusMessage(ServiceStatusMessage.SERVICE_RECORD_LOGGED_MESSAGE, recordsProcessed);
        for (IServiceStatusListener listener : serviceMessageListeners)
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

    /**
     * Notify all the listeners that we have a new PCAP Record available.
     *
     * @param message The new PCAP message to send to the listeners.
     */
    private void notifyPcapMessageListeners(PcapMessage message)
    {
        if (message == null) return;
        for (IPcapMessageListener listener : messageListeners)
        {
            try
            {
                listener.onPcapMessage(message);
            } catch (Exception e)
            {
                Timber.e(e, "Unable to notify a PCAP Message Listener because of an exception");
            }
        }
    }
}
