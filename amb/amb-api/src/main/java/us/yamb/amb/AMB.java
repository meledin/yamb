package us.yamb.amb;

import us.yamb.amb.builders.ChannelBuilder;
import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.mb.MBMethods;
import us.yamb.mb.Observable;

/**
 * Abstract Message Bus is a generic class to use in lieu of a specific message
 * bus. Providers can implement the requisite interfaces and facilitate the bus
 * usage.
 */
public interface AMB extends MBMethods<AMBStatus, ChannelBuilder, Send>,Observable<AMBCallbackInterface, AMB>
{

}
