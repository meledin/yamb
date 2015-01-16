package us.yamb.rmb.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map.Entry;

import us.yamb.mb.util.StringUtil;

public class RMBMessage
{
	public enum Header
	{
		To(1), From(2), ContentType(3), Method(4), Custom(255);

		private int	id;

		private Header(int id)
		{
			this.id = id;
		}

		public int getId()
		{
			return id;
		}

		public static Header fromId(int id)
		{

			switch (id)
			{
				case 1:
					return To;
				case 2:
					return From;
				case 3:
					return ContentType;
				case 4:
					return Method;
				default:
					return null;
			}

		}

	}

	//@formatter:off
	
	/*
	 * Message pack format:
	 * 
	 * uint32 		- total length
	 * uint8 		- num headers [A]
	 * uint8 		- rsv1
	 * uint8 		- rsv2
	 * uint8 		- rsv3 // (Also pads the array access to 32 bits)
	 * 
	 * ## REP * A ##
	 * 
	 * uint16 		- header ID/header name length (if > 255, val-255 = length)
	 * uint16		- header value length
	 * 
	 * ## END REP ##
	 * 
	 * ## REP * A ##
	 * UTF8String	- Header Name (if applicable)
	 * UTF8String	- Header Value (if applicable)
	 * ## END REP ##
	 * 
	 * uint32		- body length (should add up to total - complete header length)
	 * 
	 * byte[] body
	 * 
	 */
	//@formatter:on

	HashMap<Header, String>	headers	      = new HashMap<RMBMessage.Header, String>();
	HashMap<String, String>	customHeaders	= new HashMap<String, String>();
	byte[]	                body	      = new byte[0];

	public byte[] bytes()
	{
		return body;
	}

	public void bytes(byte[] body)
	{
		this.body = body;
	}

	public String header(Header header)
	{
		return headers.get(header);
	}

	public String toString()
	{

		StringBuilder sb = new StringBuilder();

		for (Entry<Header, String> e : headers.entrySet())
		{
			sb.append(e.getKey().toString());
			sb.append(": ");
			sb.append(e.getValue());
			sb.append("\n");
		}

		for (Entry<String, String> e : customHeaders.entrySet())
		{
			sb.append(e.getKey());
			sb.append(": ");
			sb.append(e.getValue());
			sb.append("\n");
		}

		sb.append("\n");
		sb.append(body.length + " bytes in body");

		return sb.toString();
	}

	public byte[] pack() throws IOException
	{

		int numHeaders = headers.size() + customHeaders.size();

		int preambleLength = 8 + 4 * numHeaders;
		int headerLength = 0; // Placeholder until we can calculate this.
		int postLength = 4 + body.length;

		ByteArrayOutputStream hvStream = new ByteArrayOutputStream(numHeaders * 128);

		ByteBuffer preamble = ByteBuffer.wrap(new byte[preambleLength]);
		byte[] headers = null;
		byte[] bodyLengthBytes = ByteBuffer.allocate(4).putInt(body.length).array();

		preamble.putInt(0); // Placeholder until we can calculate the total
							// length.
		preamble.put((byte) numHeaders);
		preamble.put((byte) 0);
		preamble.put((byte) 0);
		preamble.put((byte) 0);

		for (Entry<Header, String> e : this.headers.entrySet())
		{
			preamble.putShort((short) e.getKey().getId());
			byte[] val = StringUtil.toUtfBytes(e.getValue());
			preamble.putShort((short) val.length);
			hvStream.write(val);
		}

		for (Entry<String, String> e : this.customHeaders.entrySet())
		{
			byte[] key = StringUtil.toUtfBytes(e.getKey());
			byte[] val = StringUtil.toUtfBytes(e.getValue());
			preamble.putShort((short) (255 + key.length));
			preamble.putShort((short) val.length);
			hvStream.write(key);
			hvStream.write(val);
		}

		// Headers written. Calculate the length, adjust appripriately.
		headers = hvStream.toByteArray();
		headerLength = headers.length;

		int length = preambleLength + headerLength + postLength;

		byte[] packed = new byte[length];
		int writePtr = 0;

		System.arraycopy(preamble.array(), 0, packed, 0, preambleLength);
		writePtr += preambleLength;

		System.arraycopy(headers, 0, packed, writePtr, headerLength);
		writePtr += headerLength;

		System.arraycopy(bodyLengthBytes, 0, packed, writePtr, 4);
		writePtr += 4;

		System.arraycopy(body, 0, packed, writePtr, body.length);
		
		ByteBuffer.wrap(packed, 0, 4).putInt(length);

		return packed;

	}

	public static RMBMessage unpack(byte[] bs) throws RMBException
	{

		RMBMessage msg = new RMBMessage();

		int length = ByteBuffer.wrap(bs, 0, 4).getInt();

		ByteBuffer opts = ByteBuffer.wrap(bs, 4, 4);

		int nHeaders = opts.get();
		
		int preambleLength = 8+4*nHeaders;

		int[] headerIds = new int[nHeaders];
		int[] headerLengths = new int[nHeaders];

		ByteBuffer headerInfo = ByteBuffer.wrap(bs, 8, nHeaders * 4);

		for (int i = 0; i < nHeaders; i++)
		{
			headerIds[i] = ((int) headerInfo.getShort()) & 0xFFFF;
			headerLengths[i] = ((int) headerInfo.getShort()) & 0xFFFF;
		}

		int headerOffset = 8 + nHeaders * 4;

		for (int i = 0; i < nHeaders; i++)
		{

			int hnameLength = Math.max(0, headerIds[i] - 255);
			String value = StringUtil.toUtfString(bs, headerOffset + hnameLength, headerLengths[i]);

			if (hnameLength > 0)
			{
				String name = StringUtil.toUtfString(bs, headerOffset, hnameLength);
				msg.header(name, value);
			}
			else
			{
				Header header = Header.fromId(headerIds[i]);
				msg.header(header, value);
			}

			headerOffset += hnameLength + headerLengths[i];
			preambleLength += hnameLength + headerLengths[i];
		}

		int bodyLength = ByteBuffer.wrap(bs, headerOffset, 4).getInt();
		
		if (bodyLength + preambleLength + 4 != length)
			throw new RMBException("Invalid message; expected message length of " + (bodyLength+preambleLength+4) + " but was " + length + ". This is most likely a corrupt message.");

		byte[] body = new byte[bodyLength];
		System.arraycopy(bs, headerOffset + 4, body, 0, bodyLength);

		msg.bytes(body);

		return msg;
	}

	public void header(Header header, String value)
	{
		headers.put(header, value);
	}

	public void header(String name, String value)
	{
		customHeaders.put(name, value);
	}

}
