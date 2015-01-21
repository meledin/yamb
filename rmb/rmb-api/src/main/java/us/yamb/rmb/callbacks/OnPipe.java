package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.Pipe;

@FunctionalInterface
public interface OnPipe
{
    public void onpipe(Pipe pipe, Message message);
}
