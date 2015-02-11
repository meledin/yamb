package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.Response.ResponseException;

@FunctionalInterface
public interface OnPost extends RMBCallbackInterface
{
    public void onpost(Message message) throws ResponseException;
}
