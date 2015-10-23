package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.Response.ResponseException;

@FunctionalInterface
public interface OnHead extends RMBCallbackInterface
{
    public void onhead(Message message) throws ResponseException;
}
