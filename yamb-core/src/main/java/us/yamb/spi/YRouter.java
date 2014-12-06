package us.yamb.spi;

import java.util.Map;

import us.yamb.amb.AMessage;

public interface YRouter
{
    public void route(AMessage message);
    
    public Map<String, Map<String, Object>> getPeerInfo();
}
