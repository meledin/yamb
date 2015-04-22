package us.yamb.rmb.impl;

import org.junit.Test;

import us.yamb.rmb.impl.PackedMessage.Header;

public class RMBMessageTest
{

	@Test
	public void testSimplePack() throws Exception
	{
		PackedMessage msg = new PackedMessage();
		msg.header(Header.To, "/foo/bar");
		msg.header(Header.From, "/bar/foo");
		System.out.println(msg.toString());
		PackedMessage unpack = PackedMessage.unpack(msg.pack());
		System.out.println(unpack.toString());
		System.err.println(unpack.toString().length());
	}
}
