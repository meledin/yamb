package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;

@FunctionalInterface
public interface OnPut extends RMBCallbackInterface
{
    public void onput(Message message);
}
