package us.yamb.amb;

import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.mb.MBChannelBase;
import us.yamb.mb.Observable;

public interface Channel extends Observable<AMBCallbackInterface, Channel>, MBChannelBase<Channel>
{
}
