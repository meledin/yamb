package us.yamb.amb;

import java.io.IOException;

import us.yamb.mb.builders.MBMessageBuilder;

public interface Send extends MBMessageBuilder<Send>
{
	
	public void send() throws IOException;
	
}
