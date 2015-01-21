package us.yamb.rmb.impl.builders;

import us.yamb.rmb.Send;
import us.yamb.rmb.impl.RMBImpl;

public class SendBuilder extends SendImpl<Send> implements Send
{

    public SendBuilder(RMBImpl parent, us.yamb.amb.Send aSend)
    {
        super(parent, aSend);
    }
    
}
