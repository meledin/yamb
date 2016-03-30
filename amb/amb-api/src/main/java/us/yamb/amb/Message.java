package us.yamb.amb;

/**
 * YMessage describes the actual communication that trigered a Yamb call.
 */
public interface Message
{
    public String from();
    
    public String to();
    
    public byte[] bytes();
    
    public String string();
    
    public <T> T object(Class<T> baseClass);
    
    public boolean confirmed();
    
    public String id();
    
}
