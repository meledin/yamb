package us.yamb.rmb.builders;

import us.yamb.mb.builders.MBMessageBuilder;
import us.yamb.rmb.Location;
import us.yamb.rmb.Send;

public interface RestMessageBuilder<T> extends MBMessageBuilder<T>
{
	T method(String method);
	
	T to(Location to);

    T status(int status);
}
