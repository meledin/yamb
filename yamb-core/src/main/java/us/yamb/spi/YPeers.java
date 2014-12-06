package us.yamb.spi;

import java.util.Collection;
import java.util.Map;

import us.yamb.amb.AMessage;

public interface YPeers
{
    
    public static final String ONE_TIME_TOKEN = "PEER_POLICY_ONE_TIME_TOKEN";
    public static final String STATIC_TOKEN   = "PEER_POLICY_STATIC_TOKEN";
    
    public YPeers addPeer(String peerInfo);
    
    public YPeers removePeer(String peerInfo);
    
    public YPeers setPeerPolicy(String policy);
    
    public String setPeerToken(String token);
    
    public String getPeerInfo();
    
    public YPeers enableAllTransports();
    
    public YPeers disableAllTransports();
    
    public Collection<String> getTransportNames();
    
    public Collection<String> getTransportProperties(String transport);
    
    public String getTransportProperty(String transport, String value);
    
    public YPeers configure(String transport, String property, String value);
    
    /**
     * Fetches all the available context, for all transports; including, where available, IP numbers, ports, etc. This
     * context will consist of a map for each transport, indexed by transport name. This information will provide detail
     * level information on the connection for each transport.
     * <p>
     * Typical output for a single-transport scenario may look as follows:
     * 
     * <pre>
     * {
     *  socket= {
     *      LastAlive=1407198698446,
     *      Format=REGULAR, 
     *      State=AVAILABLE, 
     *      Transport=socket/1/AVAILABLE/704f459c, 
     *      LocalIP=127.0.0.1, 
     *      LocalPort=59277, 
     *      Configuration=trap.enablecompression = true
     *      RemotePort=59278, 
     *      Protocol=tcp, 
     *      RemoteIP=127.0.0.1, 
     *      Priority=-100
     *      }
     *  }
     * </pre>
     * 
     * From this information, we can gather all pertinent information about the transport's internal state. This is a
     * socket transport that is not configured to connect, so was provided by a server endpoint. The state is AVAILABLE,
     * and lastAlive specifies when there was traffic seen. We have the TCP 5-tuple information, which can be used to
     * identify a connection that has not changed.
     * <p>
     * This method is intended for <i>informational</i> purposes. The returned values are immediately polled, will
     * include all possible values, and will be requested synchronously; the CPU cost of using this method often is
     * non-negligible. The method is thread safe with no need for synchronization.
     * 
     * @return A map of all transport context available.
     */
    public Map<String, Map<String, Object>> getTransportContext();
    
    public void send(AMessage msg);
}
