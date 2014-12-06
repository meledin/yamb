package us.yamb.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import us.yamb.amb.AMessage;
import us.yamb.spi.YPeer;
import us.yamb.spi.YPeers;

public class YPeersImpl implements YPeers
{
    
    Map<String, YPeer> peers = new HashMap<String, YPeer>();
    
    public YPeers addPeer(String peerInfo)
    {
        YPeerImpl peer = new YPeerImpl(peerInfo);
        return this;
    }
    
    public YPeers removePeer(String peerInfo)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public YPeers setPeerPolicy(String policy)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public String setPeerToken(String token)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public String getPeerInfo()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public YPeers enableAllTransports()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public YPeers disableAllTransports()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Collection<String> getTransportNames()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Collection<String> getTransportProperties(String transport)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public String getTransportProperty(String transport, String value)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public YPeers configure(String transport, String property, String value)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Map<String, Map<String, Object>> getTransportContext()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void send(AMessage msg)
    {
        // TODO Auto-generated method stub
        
    }
    
}
