package us.yamb.amb.builders;

import us.yamb.amb.Channel;

public interface ChannelBuilder
{
    ChannelBuilder name(String name);
    Channel build();
}
