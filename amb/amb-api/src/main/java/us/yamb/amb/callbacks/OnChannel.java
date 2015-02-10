package us.yamb.amb.callbacks;

import us.yamb.amb.AMB;
import us.yamb.amb.Channel;
import us.yamb.amb.Message;

public interface OnChannel extends AMBCallbackInterface
{
    public void onchannel(AMB amb, Channel channel, Message message);
}
