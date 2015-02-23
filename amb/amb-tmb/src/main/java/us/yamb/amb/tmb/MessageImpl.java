package us.yamb.amb.tmb;

import us.yamb.amb.Message;

import com.ericsson.research.trap.utils.StringUtil;

public class MessageImpl implements Message
{

	String from;
	String to;
	String id;
	byte[] payload;

	public String from()
	{
		return from;
	}

	public String to()
	{
		return to;
	}

	public byte[] bytes()
	{
		return payload;
	}

	public String string()
	{
		return StringUtil.toUtfString(payload);
	}

	public <T> T object(Class<T> baseClass)
	{
		return null;
	}

	public boolean confirmed()
	{
		return false;
	}

	public String id()
	{
		return id;
	}

	public void setFrom(String from)
	{
		this.from = from;
	}

	public void setTo(String to)
	{
		this.to = to;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setPayload(byte[] payload)
	{
		this.payload = payload;
	}

    @Override
    public String toString()
    {
        return from + "->" + to;
    }

}
