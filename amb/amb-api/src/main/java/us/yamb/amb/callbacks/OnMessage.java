package us.yamb.amb.callbacks;

import us.yamb.amb.AMB;
import us.yamb.amb.Message;

public interface OnMessage extends AMBCallbackInterface
{
    public void onmessage(AMB amb, Message message);
}
