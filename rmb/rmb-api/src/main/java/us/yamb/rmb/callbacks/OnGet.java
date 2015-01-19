package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;

@FunctionalInterface
public interface OnGet extends RMBCallbackInterface
{
    public void onget(Message message);
}
