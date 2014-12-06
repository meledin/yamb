package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.RMB;

public interface OnMessage extends RMBCallbackInterface
{

    public void onmessage(RMB rmb, Message message);
}
