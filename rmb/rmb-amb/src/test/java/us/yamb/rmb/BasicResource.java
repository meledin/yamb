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
    @Path("")
    public String hello()
    {
        return "Hello!";
    }
    
    @GET
    @Path("test")
    public String hi()
    {
        return "Hi!";
    }
    
    @GET
    @Path("{foo}/foo")
    public String foo()
    {
        return "foo";
    }
    
    @GET
    @Path("{foo}/bar")
    public String bar()
    {
        return "bar";
    }
    
    @POST
    public String process(Map<String, String> bodyParams) {
        System.out.println("Process");
        return bodyParams.toString();
    }
    
}
