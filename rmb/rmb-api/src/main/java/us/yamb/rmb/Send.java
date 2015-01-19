package us.yamb.rmb;

import java.io.IOException;

import us.yamb.rmb.builders.RestMessageBuilder;

public interface Send extends RestMessageBuilder<Send>
{
    public void send() throws IOException;
}
