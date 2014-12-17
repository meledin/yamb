package us.yamb.mb;

import java.io.IOException;

import us.yamb.mb.callbacks.AsyncResult;

public interface MBChannelBase<T>
{
	public String name();

	public T send(byte[] data) throws IOException;

	public T send(String data) throws IOException;

	public T send(Object json) throws IOException;

	public AsyncResult<Boolean> join();

	public void leave();
}
