package us.yamb.amb.spi;

import us.yamb.amb.Channel;
import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.amb.callbacks.OnChannel;

public abstract class ChannelBase extends ObservableBase<AMBCallbackInterface, Channel> implements Channel
{
	protected OnChannel onchannel;
}
