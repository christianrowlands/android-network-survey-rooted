package com.craxiom.networksurveyplus;

/**
 * An interface for listening to messages sent out by the main service {@link QcdmService}.
 */
public interface IServiceStatusListener
{
    void onServiceStatusMessage(ServiceStatusMessage serviceMessage);
}
