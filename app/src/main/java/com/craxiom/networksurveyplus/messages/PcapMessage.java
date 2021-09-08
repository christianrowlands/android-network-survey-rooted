package com.craxiom.networksurveyplus.messages;

/**
 * A simple wrapper for PCAP formatted byte arrays to allow for additional information to tag along with the pcap
 * record.
 *
 * @since 0.5.0
 */
public class PcapMessage
{
    public static final int UNSET_CHANNEL_TYPE = -1;

    private final byte[] pcapRecord;
    private final String messageType;
    private final int channelType;

    /**
     * Constructor for when the Channel Type does not need to be set.
     *
     * @param pcapRecord  The pcap record bytes.
     * @param messageType The message type that follows the Network Survey Messaging API specification.
     */
    public PcapMessage(byte[] pcapRecord, String messageType)
    {
        this(pcapRecord, messageType, UNSET_CHANNEL_TYPE);
    }

    /**
     * Full constructor with all the parameters.
     *
     * @param pcapRecord  The pcap record bytes.
     * @param messageType The message type that follows the Network Survey Messaging API specification.
     * @param channelType The GSMTAP Channel Type of the Cellular Logical Channel that this message was sent over. This
     *                    GSM Channel Type (Sometimes call subtype) is represented as an integer. More information on
     *                    the GSMTAP channel (sub)types, see https://osmocom.org/projects/libosmocore/repository/revisions/master/entry/include/osmocom/core/gsmtap.h
     */
    public PcapMessage(byte[] pcapRecord, String messageType, int channelType)
    {
        this.pcapRecord = pcapRecord;
        this.messageType = messageType;
        this.channelType = channelType;
    }

    /**
     * @return True if the channel type is set, false otherwise.
     */
    public boolean hasChannelType()
    {
        return channelType != UNSET_CHANNEL_TYPE;
    }

    public byte[] getPcapRecord()
    {
        return pcapRecord;
    }

    /**
     * @return The message type that follows the Network Survey Messaging API specification.
     */
    public String getMessageType()
    {
        return messageType;
    }

    /**
     * @return The GSMTAP Channel Type of the Cellular Logical Channel that this message was sent over. This
     * GSM Channel Type (Sometimes call subtype) is represented as an integer. More information on
     * the GSMTAP channel (sub)types, see https://osmocom.org/projects/libosmocore/repository/revisions/master/entry/include/osmocom/core/gsmtap.h
     */
    public int getChannelType()
    {
        return channelType;
    }
}
