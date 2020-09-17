package com.craxiom.networksurveyplus;

public class ServiceMessage
{
    public int what;
    public Object data;

    public ServiceMessage(int what, Object data)
    {
        this.what = what;
        this.data = data;
    }
}
