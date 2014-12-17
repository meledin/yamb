package us.yamb.rmb;

/**
 * YMessage describes the actual communication that trigered a Yamb call.
 */
public interface Message
{
    public Path from();
    
    public Path to();
    
    public String method();
    
    public String header(String name);
    
    public byte[] bytes();
    
    public String string();
    
    public <T> T object(Class<T> baseClass);
    
    public boolean confirmed();
    
    public String id();
    
}
