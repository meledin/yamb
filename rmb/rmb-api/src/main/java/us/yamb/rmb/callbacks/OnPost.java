package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;

@FunctionalInterface
public interface OnPost extends RMBCallbackInterface
{
    public void onpost(Message message);
}
