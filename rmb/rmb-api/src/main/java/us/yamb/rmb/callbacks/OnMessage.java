package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.Response.ResponseException;

@FunctionalInterface
public interface OnMessage extends RMBCallbackInterface
{
    public void onmessage(Message message) throws ResponseException;
}
