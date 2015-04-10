package us.yamb.rmb;

import java.util.Map;

/**
 * The Message interface allows the reading of the data of an incoming message.
 */
public interface Message
{
    
    /**
     * The <i>from</i> parameter contains the sender of this message. Any replies should be directed to this {@link Location}.
     * 
     * @return The location of the sending resource
     */
    public Location from();
    
    /**
     * The recipient of the message. Generally, this is the RMB instance that received the message.
     * 
     * @return The Location of the receiving resource.
     */
    public Location to();
    
    /**
     * The method applied. May be <i>null</i> if REST semantics are not used fully.
     * 
     * @return The method applied.
     */
    public String method();
    
    /**
     * Accessor for the headers. Gets the header with <i>name</i>. Headers are case sensitive.
     * 
     * @param name
     *            The header name to retrieve
     * @return The value of the header <i>name</i>
     */
    public String header(String name);
    
    /**
     * The raw data payload of the message.
     * 
     * @return A byte array containing the full payload.
     */
    public byte[] bytes();
    
    /**
     * Convert the paylod of the message into a String, using UTF-8 encoding
     * 
     * @return The String representation of the payload
     */
    public String string();
    
    /**
     * The status code, if any, of the message. If no status code has been set, will be 0.
     * 
     * @return The status code.
     */
    public int status();
    
    /**
     * Parse the payload of the message as JSON and return the object.
     * 
     * @param baseClass
     *            The class to use for JSON deserialisation.
     * @return The object, as read from the JSON.
     */
    public <T> T object(Class<T> baseClass);
    
    /**
     * Whether this message was confirmed on sending
     * 
     * @return
     */
    public boolean confirmed();
    
    /**
     * A unique message identifier, when applicable
     * 
     * @return
     */
    public long id();
    
    /**
     * The message headers, if any.
     * 
     * @return
     */
    public Map<String, String> headers();
    
}
