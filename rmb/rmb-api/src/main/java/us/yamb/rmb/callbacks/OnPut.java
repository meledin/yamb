package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.Response.ResponseException;

@FunctionalInterface
public interface OnPut extends RMBCallbackInterface
{
    public void onput(Message message) throws ResponseException;
}
