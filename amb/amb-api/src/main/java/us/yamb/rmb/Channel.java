package us.yamb.rmb;

import us.yamb.mb.Observable;
import us.yamb.rmb.callbacks.RMBCallbackInterface;

public interface Channel extends Observable<RMBCallbackInterface, Channel>
{
	public String name();
	
    public Channel send(byte[] data);
    
    public Channel send(String data);
    
    public Channel send(Object json);
    
    public Channel join();
    
    public Channel leave();
}
