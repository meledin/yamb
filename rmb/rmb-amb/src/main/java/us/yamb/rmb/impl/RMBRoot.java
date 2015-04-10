package us.yamb.rmb.impl;

import java.io.IOException;

import us.yamb.amb.AMB;
import us.yamb.amb.Channel;
import us.yamb.amb.Message;
import us.yamb.amb.callbacks.OnChannel;
import us.yamb.amb.callbacks.OnConnect;
import us.yamb.amb.callbacks.OnMessage;
import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.RMBStatus;
import us.yamb.rmb.Request;
import us.yamb.rmb.Send;
import us.yamb.rmb.impl.builders.RequestImpl;
import us.yamb.rmb.impl.builders.SendBuilder;

import com.ericsson.research.trap.utils.ThreadPool;

public class RMBRoot extends RMBImpl implements OnConnect, OnChannel, OnMessage
{
    
    private AMB amb;
    
    public RMBRoot(AMB amb)
    {
        this.amb = amb;
        amb.setCallback(this);
    }
    
    public String id()
    {
        if (name == null)
            throw new IllegalStateException("Cannot get ID of RMB that is not connected");
        
        return "/" + name;
    }
    
    public AsyncResult<Exception> connect()
    {
        return amb.connect();
    }
    
    public void disconnect()
    {
        amb.disconnect();
    }
    
    public RMBStatus status()
    {
        switch (amb.status())
        {
            case CONNECTED:
                return RMBStatus.CONNECTED;
            case CONNECTING:
                return RMBStatus.CONNECTING;
            case DISCONNECTED:
            default:
                return RMBStatus.DISCONNECTED;
                
        }
    }
    
    public void onconnect(AMB amb)
    {
        this.name = amb.id();
    }
    
    @Override
    public Send message(RMBImpl res)
    {
        return new SendBuilder(res, amb.message());
    }
    
    @Override
    public String seedInfo()
    {
        return amb.seedInfo();
    }
    
    @Override
    public void onmessage(AMB amb, Message message)
    {
        
        ThreadPool.executeCached(new Runnable() {
            
            @Override
            public void run()
            {
                RMBMessage<?> msg = RMBMessage.deserialize(message.bytes());
                try
                {
                    if (!dispatch(msg, 1))
                    {
                        try
                        {
                            if (msg.status() < 100)
                                message().to(msg.from()).status(404).send();
                            else if (msg.status() >= 300)
                                System.err.println(msg);
                        }
                        catch (IOException e)
                        {
                        }
                    }
                }
                catch (Exception e)
                {
                    
                    try
                    {
                        e.printStackTrace();
                        message().to(msg.from()).status(500).data(e.getMessage()).send();
                    }
                    catch (IOException e1)
                    {
                    }
                }
            }
        });
    }
    
    @Override
    public void onchannel(AMB amb, Channel channel, Message message)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public Request request(RMBImpl res)
    {
        return new RequestImpl(res, amb.message());
    }
    
    @Override
    public us.yamb.amb.Send _ambSend()
    {
        return amb.message();
    }
    
    @Override
    public void remove()
    {
        // No effect on root
    }
}
