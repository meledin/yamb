package us.yamb.spi;

import java.util.Map;

import us.yamb.amb.AMessage;

import com.ericsson.research.trap.utils.Callback;

public interface YPeer
{
    
    /**
     * All peers in the Yamb network have an assigned coordinate, which allows the network to route them.
     */
    public interface Coordinates
    {
        double x();
        
        double y();
        
        double z();
        
        void setX(double x);
        
        void setY(double y);
        
        void setZ(double z);
    }
    
    enum PeerConnectResult {
        SUCCESS,
        AUTH_FAILURE,
        NET_FAILURE
    }
    
    public Callback<PeerConnectResult> connect();
    
    /**
     * All peers also have a HashID, which provides a unique yamb:hash URI to identify the node. This hash can be used
     * in DHT configurations.
     * 
     * @return The peer's hash.
     */
    public String getPeerHash();
 
    public Coordinates getCoordinates();
    
    public Map<String, String> getPeerInfo();
    
    public void send(AMessage message);
}
