package us.yamb.amb.builders;

public interface MessageBuilder<T>
{

    public T to(String id);
    
    public T data(byte[] data);
    
    public T data(String data);
    
    public T data(Object data);
    
    /**
     * Specify whether the message should be <i>confirmed</i>. A confirmed message will receive back a receipt
     * confirmation, or retry sending. There is no guarantee that this message will be received, but several attempts
     * will be made.
     * 
     * @param confirmed
     *            If the message receipt is to be confirmed.
     * @return This builder, for chaining.
     */
    public T confirmed(boolean confirmed);
}
