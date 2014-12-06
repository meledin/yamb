package us.yamb.amb.callbacks;

import us.yamb.amb.Message;
import us.yamb.amb.AMB;
import us.yamb.amb.Channel;

public interface OnChannel extends AMBCallbackInterface
{
    public void onchannel(AMB amb, Channel channel, Message message);
}
