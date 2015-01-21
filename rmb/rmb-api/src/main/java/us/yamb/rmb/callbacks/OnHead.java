package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;

@FunctionalInterface
public interface OnHead extends RMBCallbackInterface
{
    public void onhead(Message message);
}
