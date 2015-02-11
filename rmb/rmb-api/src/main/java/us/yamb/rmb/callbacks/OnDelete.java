package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.Response.ResponseException;

@FunctionalInterface
public interface OnDelete extends RMBCallbackInterface
{
    public void ondelete(Message message) throws ResponseException;
}
