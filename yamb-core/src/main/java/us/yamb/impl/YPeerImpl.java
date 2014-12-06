package us.yamb.impl;

import java.util.Map;

import us.yamb.amb.AMessage;
import us.yamb.spi.YPeer;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.utils.Callback;
import com.ericsson.research.trap.utils.spi.ConfigurationImpl;

public class YPeerImpl implements YPeer
{
    
    private ConfigurationImpl peerCfg;
    private TrapEndpoint peer;

    public YPeerImpl(String peerInfo)
    {
        this.peerCfg = new ConfigurationImpl(peerInfo);
    }

    public YPeerImpl(TrapEndpoint peer)
    {
        this.peer = peer;
    }

    public String getPeerHash()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Coordinates getCoordinates()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Map<String, String> getPeerInfo()
    {
        return this.peerCfg.getOptions("", false);
    }
    
    public void send(AMessage message)
    {
        try
        {
            this.peer.send(message);
        }
        catch (TrapException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Callback<PeerConnectResult> connect()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
}
