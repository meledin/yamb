package us.yamb.rmb.impl;

import java.util.UUID;

import org.junit.Test;

import us.yamb.rmb.impl.PackedMessage.Header;

public class RMBMessageTest
{

	@Test
	public void testSimplePack() throws Exception
	{
		PackedMessage msg = new PackedMessage();
		msg._header(Header.To, "/foo/bar");
		msg._header(Header.From, "/bar/foo");
		msg.body = UUID.randomUUID().toString().getBytes();
		System.out.println(msg.toString());
		PackedMessage unpack = PackedMessage.unpack(msg.pack(), new PackedMessage());
		System.out.println(unpack.toString());
		System.err.println(unpack.toString().length());
	}
}
