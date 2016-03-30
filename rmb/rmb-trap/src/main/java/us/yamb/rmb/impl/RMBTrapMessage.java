package us.yamb.rmb.impl;

import java.util.HashMap;

import us.yamb.mb.util.JSON;
import us.yamb.mb.util.StringUtil;

import com.ericsson.research.trap.TrapObject;

public class RMBTrapMessage extends HashMap<String, String> implements TrapObject
{
    
    public static final String REQUESTED_ID         = "REGISTERED_ID";
    public static final String OPERATION_REGISTER   = "OPERATION_REGISTER";
    public static final String OPERATION_REGISTERED = "OPERATION_REGISTERED";
    public static final String OP_TYPE              = "OP_TYPE";
    public static final String APPROVED_ID          = "APPROVED_ID";
    private static final long  serialVersionUID     = 1L;
    
    @Override
    public byte[] getSerializedData()
    {
        return StringUtil.toUtfBytes(JSON.toJSON(this));
    }
    
    public static RMBTrapMessage parse(byte[] buf)
    {
        RMBTrapMessage msg = new RMBTrapMessage();
        msg.putAll(JSON.fromJSON(StringUtil.toUtfString(buf), RMBTrapMessage.class));
        return msg;
    }
    
}
