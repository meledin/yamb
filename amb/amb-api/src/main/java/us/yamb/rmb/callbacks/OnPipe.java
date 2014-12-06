package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.Pipe;
import us.yamb.rmb.RMB;

public interface OnPipe
{
    public void onpipe(RMB rmb, Pipe pipe, Message message);
}
