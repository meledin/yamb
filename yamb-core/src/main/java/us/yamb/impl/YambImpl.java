package us.yamb.impl;

import us.yamb.amb.AMB;
import us.yamb.amb.AMBStatus;
import us.yamb.amb.Send;
import us.yamb.amb.builders.ChannelBuilder;
import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.rmb.Request;
import us.yamb.spi.YPeers;

public class YambImpl implements AMB
{

    public String id()
    {
        return null;
    }

    public AMB create()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public AMB create(String subId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ChannelBuilder channel(String targetId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Send send()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Request request()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public AMBStatus status()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void connect()
    {
        // TODO Auto-generated method stub
        
    }

    public void disconnect()
    {
        // TODO Auto-generated method stub
        
    }

    public void setCallback(AMBCallbackInterface callback)
    {
        // TODO Auto-generated method stub
        
    }

    public YPeers peers()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
}
