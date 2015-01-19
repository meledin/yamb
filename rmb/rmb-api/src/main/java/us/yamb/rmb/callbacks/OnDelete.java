package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;

@FunctionalInterface
public interface OnDelete extends RMBCallbackInterface
{
    public void ondelete(Message message);
}
