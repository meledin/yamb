package us.yamb.amb;

import java.io.IOException;

import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.amb.callbacks.AsyncResult;
import us.yamb.mb.Observable;

public interface Channel extends Observable<AMBCallbackInterface, Channel>
{
	public String name();
	
    public Channel send(byte[] data) throws IOException;
    
    public Channel send(String data) throws IOException;
    
    public Channel send(Object json) throws IOException;
    
    public AsyncResult<Boolean> join();
    
    public void leave();
}
