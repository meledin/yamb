package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.RMB;

@FunctionalInterface
public interface OnPost extends RMBCallbackInterface
{
    public void onpost(Message message);
}
