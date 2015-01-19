package us.yamb.rmb.impl.builders;

import java.io.IOException;

import us.yamb.rmb.Location;
import us.yamb.rmb.Send;
import us.yamb.rmb.impl.RMBImpl;
import us.yamb.rmb.impl.RMBMessage;

public class SendImpl extends RMBMessage<Send> implements Send
{

	private RMBImpl parent;
    private us.yamb.amb.Send msg;
	
	public SendImpl(RMBImpl parent, us.yamb.amb.Send aSend)
    {
		this.parent = parent;
        this.msg = aSend;
    }

    @Override
    public void send() throws IOException
    {
        from(parent.id());
        msg.confirmed(confirmed());
        msg.data(serialize());
        msg.to(to().getPart(Location.ROOT_ID));
        msg.send();
    }
    
}
