package us.yamb.rmb;

import java.util.Map;

import us.yamb.rmb.annotations.GET;
import us.yamb.rmb.annotations.POST;
import us.yamb.rmb.annotations.Path;

@Path("")
public class BasicResource
{
    RMB res;
    
    public BasicResource(RMB res)
    {
        this.res = res;
    }
    
    @GET
    public String hello()
    {
        return "Hello!";
    }
    
    @POST
    public String process(Map<String, String> bodyParams) {
        return bodyParams.toString();
    }
    
}
