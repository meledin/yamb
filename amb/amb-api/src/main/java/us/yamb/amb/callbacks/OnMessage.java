package us.yamb.amb.callbacks;

import us.yamb.amb.Message;
import us.yamb.amb.AMB;

public interface OnMessage extends AMBCallbackInterface
{
    public void onmessage(AMB amb, Message message);
}
