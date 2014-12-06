package us.yamb.rmb.builders;

import us.yamb.amb.builders.MessageBuilder;
import us.yamb.rmb.Path;

public interface RestMessageBuilder<T> extends MessageBuilder<T>
{
	T method(String method);
	
	T from(Path from);
	
	T to(Path to);
}
