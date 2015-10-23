package us.yamb.rmb.impl.builders;

import us.yamb.rmb.Send;
import us.yamb.rmb.impl.RMBImpl;
import us.yamb.rmb.impl.RMBRootImpl;

public class SendBuilder extends SendImpl<Send> implements Send
{
    
    public SendBuilder(RMBImpl parent, RMBRootImpl rmbRoot)
    {
        super(parent, rmbRoot);
    }
    
}
