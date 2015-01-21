package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;

@FunctionalInterface
public interface OnMessage extends RMBCallbackInterface
{
    public void onmessage(Message message);
}
