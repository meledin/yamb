package us.yamb.rmb.impl;

import org.junit.Test;

import us.yamb.rmb.impl.RMBMessage.Header;

public class RMBMessageTest
{

	@Test
	public void testSimplePack() throws Exception
	{
		RMBMessage msg = new RMBMessage();
		msg.header(Header.To, "/foo/bar");
		msg.header(Header.From, "/bar/foo");
		System.out.println(msg.toString());
		RMBMessage unpack = RMBMessage.unpack(msg.pack());
		System.out.println(unpack.toString());
		System.err.println(unpack.toString().length());
	}
}
