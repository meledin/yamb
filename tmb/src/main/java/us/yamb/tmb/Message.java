package us.yamb.tmb;

import java.nio.ByteBuffer;

import com.ericsson.research.trap.utils.StringUtil;

class Message
{
	
	class Operation
	{
		public static final byte HELLO = 0;
		public static final byte SEND = 1;
		public static final byte PUB = 2;
		public static final byte SUB = 3;
		public static final byte UNSUB = 4;
		public static final byte BYE = 100;
	}

	public byte op = Operation.HELLO;
    public String to = "";
    public String from = "";
    public String channel = "";
    public byte[] payload = new byte[0];
    
    public byte[] serialize()
    {
    	int length = 0;
    	byte[] toBytes = StringUtil.toUtfBytes(to);
    	byte[] fromBytes = StringUtil.toUtfBytes(from);
    	byte[] channelBytes = StringUtil.toUtfBytes(channel);
    	
    	length = toBytes.length + fromBytes.length + channelBytes.length + payload.length + 5*4 + 1 ;
    	
    	byte[] rv = new byte[length];
    	
    	ByteBuffer buffer = ByteBuffer.wrap(rv);
    	
    	buffer.putInt(length);
    	buffer.put(op);
    	buffer.putInt(toBytes.length);
    	buffer.put(toBytes);
    	buffer.putInt(fromBytes.length);
    	buffer.put(fromBytes);
    	buffer.putInt(channelBytes.length);
    	buffer.put(channelBytes);
    	buffer.putInt(payload.length);
    	buffer.put(payload);
    	
    	return rv;
    }
    
    public static Message deserialize(ByteBuffer src)
    {
    	
    	if (src.remaining() < 4)
    		return null;
    	
    	int msgLength = src.getInt();
    	
    	if (src.remaining() + 4 < msgLength)
    		return null;
    	
    	Message m = new Message();
    	
    	m.op = src.get();
    	
    	byte[] buf = new byte[src.getInt()];
    	src.get(buf, 0, buf.length);
    	m.to = StringUtil.toUtfString(buf);
    	
    	buf = new byte[src.getInt()];
    	src.get(buf, 0, buf.length);
    	m.from = StringUtil.toUtfString(buf);
    	
    	buf = new byte[src.getInt()];
    	src.get(buf, 0, buf.length);
    	m.channel = StringUtil.toUtfString(buf);
    	
    	buf = new byte[src.getInt()];
    	src.get(buf, 0, buf.length);
    	m.payload = buf;
    	
    	return m;
    	
    }

}
