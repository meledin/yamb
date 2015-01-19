package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.RMB;

@FunctionalInterface
public interface OnHead extends RMBCallbackInterface
{
    public void onhead(Message message);
}
