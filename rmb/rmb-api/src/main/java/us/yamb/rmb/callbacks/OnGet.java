package us.yamb.rmb.callbacks;

import us.yamb.rmb.Message;
import us.yamb.rmb.Response.ResponseException;

@FunctionalInterface
public interface OnGet extends RMBCallbackInterface
{
    public void onget(Message message) throws ResponseException;
}
