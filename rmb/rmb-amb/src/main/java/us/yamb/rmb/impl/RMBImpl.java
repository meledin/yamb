package us.yamb.rmb.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import us.yamb.rmb.Location;
import us.yamb.rmb.RMB;
import us.yamb.rmb.Request;
import us.yamb.rmb.Response;
import us.yamb.rmb.Response.ResponseException;
import us.yamb.rmb.Send;
import us.yamb.rmb.builders.ChannelBuilder;
import us.yamb.rmb.builders.PipeBuilder;
import us.yamb.rmb.callbacks.OnDelete;
import us.yamb.rmb.callbacks.OnDisconnect;
import us.yamb.rmb.callbacks.OnGet;
import us.yamb.rmb.callbacks.OnHead;
import us.yamb.rmb.callbacks.OnMessage;
import us.yamb.rmb.callbacks.OnPipe;
import us.yamb.rmb.callbacks.OnPost;
import us.yamb.rmb.callbacks.OnPut;
import us.yamb.rmb.callbacks.RMBCallbackInterface;

import com.ericsson.research.trap.utils.UID;

public abstract class RMBImpl implements RMB
{
    
    public static void main(String[] args)
    {
        new RMBRoot(null).onmessage((message) -> {
            System.out.println(message);
        });
    }
    
    protected String                              name;
    protected OnPipe                              onpipe;
    protected OnPost                              onpost;
    protected OnDelete                            ondelete;
    protected OnGet                               onget;
    protected OnPut                               onput;
    protected OnDisconnect                        ondisconnect;
    protected OnMessage                           onmessage;
    protected OnHead                              onhead;
    
    protected Predicate<String>                   pathMatcher = path -> {
        if (name == null || path == null)
        {
            System.err.println("Name-or-path is null: " + name + ", " + path);
            return false;
        }
       return name.equals(path);
    };
    
    protected ConcurrentHashMap<String, RMBChild> children    = new ConcurrentHashMap<>();
    protected LinkedList<Object>                  objects     = new LinkedList<>();
    
    public ChannelBuilder channel()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public abstract Send message(RMBImpl res);
    
    public abstract Request request(RMBImpl res);
    
    public Request request()
    {
        return request(this);
    }
    
    public abstract String seedInfo();
    
    public Send message()
    {
        return message(this);
    }
    
    public RMB setCallback(RMBCallbackInterface callback)
    {
        if (callback instanceof OnPipe)
            onpipe((OnPipe) callback);
        if (callback instanceof OnPost)
            onpost((OnPost) callback);
        if (callback instanceof OnDelete)
            ondelete((OnDelete) callback);
        if (callback instanceof OnGet)
            onget((OnGet) callback);
        if (callback instanceof OnPut)
            onput((OnPut) callback);
        if (callback instanceof OnDisconnect)
            ondisconnect((OnDisconnect) callback);
        if (callback instanceof OnMessage)
            onmessage((OnMessage) callback);
        if (callback instanceof OnHead)
            onhead((OnHead) callback);
        return this;
    }
    
    public RMB setCallback(Class<? extends RMBCallbackInterface> callbackType, Object object, String function) throws NoSuchMethodException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public RMB unsetCallback(Class<? extends RMBCallbackInterface> callbackType)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public RMB create()
    {
        return create(UID.randomUID());
    }
    
    public RMB create(String id)
    {
        return create(id, false);
    }
    
    public synchronized RMB create(String id, boolean regexp)
    {
        
        if (id.startsWith("/"))
            id = id.substring(1);
        
        String[] parts = id.split("/");
        
        if (parts.length >= 2 && !regexp)
        {
            String subparts = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length)).collect(Collectors.joining("/"));
            return create(parts[0]).create(subparts);
        }
        
        if (children.get(id) != null)
            return children.get(id);
        
        RMBChild child = new RMBChild(this, id, regexp);
        
        children.put(id, child);
        
        return child;
    }
    
    public void add(Object restObject)
    {
        RestConverter.convert(this, restObject);
        this.objects.add(restObject);
    }
    
    public void add(Object restObject, Object ctx)
    {
        RestConverter.convert(this, restObject, "", ctx);
        this.objects.add(restObject);
    }
    
    public PipeBuilder pipe()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public RMB ondelete(OnDelete cb)
    {
        this.ondelete = cb;
        return this;
    }
    
    @Override
    public RMB ondisconnect(OnDisconnect cb)
    {
        this.ondisconnect = cb;
        return this;
    }
    
    @Override
    public RMB onget(OnGet cb)
    {
        onget = cb;
        return this;
    }
    
    @Override
    public RMB onhead(OnHead cb)
    {
        this.onhead = cb;
        return this;
    }
    
    @Override
    public RMB onmessage(OnMessage cb)
    {
        
        this.onmessage = cb;
        return this;
    }
    
    @Override
    public RMB onpipe(OnPipe cb)
    {
        onpipe = cb;
        return this;
    }
    
    @Override
    public RMB onpost(OnPost cb)
    {
        onpost = cb;
        return this;
    }
    
    @Override
    public RMB onput(OnPut cb)
    {
        onput = cb;
        return this;
    }
    
    protected boolean dispatch(RMBMessage<?> msg, int idx)
    {
        if (!pathMatcher.test(msg.to().getPart(idx)))
            return false;
        
        idx++;
        
        if (msg.to().parts.length == idx || pathMatcher.test(msg.to))
        {
        	try
        	{
            // This message is to me!
            switch (msg.method())
            {
                case "POST":
                case "post":
                    if (onpost != null)
                    {
                        onpost.onpost(msg);
                        return true;
                    }
                    break;
                
                case "GET":
                case "get":
                    if (onget != null)
                    {
                        onget.onget(msg);
                        return true;
                    }
                    break;
                
                case "PUT":
                case "put":
                    if (onput != null)
                    {
                        onput.onput(msg);
                        return true;
                    }
                    break;
                
                case "DELETE":
                case "delete":
                    if (ondelete != null)
                    {
                        ondelete.ondelete(msg);
                        return true;
                    }
                    break;
                
                case "HEAD":
                case "head":
                    if (onhead != null)
                    {
                        onhead.onhead(msg);
                        return true;
                    }
                    break;
            }
            
            if (onmessage != null)
            {
                onmessage.onmessage(msg);
                return true;
            }
            
            return false;
        	}
        	catch(ResponseException e) {
        		try
                {
	                e.response().send(this);
                }
                catch (IOException e1)
                {
	                e1.printStackTrace();
                }
        	}
        	catch(Exception e) {
        		try
                {
        		    e.printStackTrace();
	                Response.create(msg).status(500).data(e.getMessage()).send(this);
                }
                catch (IOException e1)
                {
	                e1.printStackTrace();
                }
        	}
            
        }
        
        // This message is for a child!
        for (RMBChild child : children.values())
        {
            if (child.dispatch(msg, idx))
                return true;
        }
        
        return false;
    }
    
    public Request get(String to)
    {
        return request().to(to).method("GET");
    }
    
    public Request get(Location to)
    {
        return get(to.toString());
    }
    
    public Request put(String to)
    {
        return request().to(to).method("PUT");
    }
    
    public Request put(Location to)
    {
        return put(to.toString());
    }
    
    public Request post(String to)
    {
        return request().to(to).method("POST");
    }
    
    public Request post(Location to)
    {
        return post(to.toString());
    }
    
    public Request delete(String to)
    {
        return request().to(to).method("DELETE");
    }
    
    public Request delete(Location to)
    {
        return delete(to.toString());
    }

    public abstract us.yamb.amb.Send _ambSend();
    
}
