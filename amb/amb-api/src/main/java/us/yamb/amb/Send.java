package us.yamb.amb;

import java.io.IOException;

import us.yamb.amb.builders.MessageBuilder;

public interface Send extends MessageBuilder<Send>
{
	
	public void send() throws IOException;
	
}
