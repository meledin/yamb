package us.yamb.rmb.builders;

import us.yamb.mb.builders.MBMessageBuilder;
import us.yamb.rmb.Path;

public interface RestMessageBuilder<T> extends MBMessageBuilder<T>
{
	T method(String method);
	
	T from(Path from);
	
	T to(Path to);
}
