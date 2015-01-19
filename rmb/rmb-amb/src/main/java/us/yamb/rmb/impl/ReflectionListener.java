package us.yamb.rmb.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.yamb.mb.util.JSON;
import us.yamb.mb.util.JSONSerializable;
import us.yamb.rmb.Message;
import us.yamb.rmb.RMB;
import us.yamb.rmb.annotations.PathParam;
import us.yamb.rmb.annotations.Produces;

/**
 * ReflectionListener is a wrapping class that allows objects to listen to messages at specific paths, with arbitrary
 * method names, and parsed parameters in the function call, in a manner similar to JSR311. For example, the following
 * methods can be called from a ReflectionListener:
 * 
 * <pre>
 * void addOne() ...
 * int getNextNumber() ...
 * String removeAllSpaces(String body) ...
 * void uploadImage(byte[] body) ...
 * void handleMessage(Message request, Resource matchingResource) ...
 * void handleAuthenticated(Message request, {@link Session} authenticated) ...
 * </pre>
 * 
 * Many more permutations are possible. Reflection listeners allow a variable <b>path</b>, parsing and reflection
 * insertion of <b>arguments</b>, arbitrary <b>return values</b> and handling of <b>exceptions</b>. We will handle each
 * case in turn below.
 * <p>
 * <h2>Reflected Path</h2> We use a JSR311-like path for matching purposes. A reflection listener will handle parsing
 * the path, inserting PathParameters as applicable, as well as parsing the arguments to the correct type. Here are some
 * acceptable paths:
 * 
 * <pre>
 * /foo/bar
 * /foo/{param}/bar
 * /foo/{param: [a-zA-Z]*}/bar
 * </pre>
 * 
 * Reflections will handle plain paths. Any parameter named {param} will be assumed to be a regular parameter,
 * consisting of regular URI-permitted characters. A regular expression can be specified using the syntax {param_name:
 * regexp}; the pattern matched by this regular expression will be bound to the named parameter.
 * <p>
 * <h2>Argument Insertion</h2> The ReflectionListener will automatically attempt to map relevant values to function
 * argument names, using the most relevant representation possible. The listener supports the automatic parsing and
 * mapping of the following types:
 * 
 * <pre>
 * Numbers (and their corresponding Java classes, e.g. int and Integer): int, long, double, float
 * Strings: UTF-8 encoding assumed
 * Message: The {@link Message} object associated with the request
 * RMB: The {@link RMB} object that captured the request.
 * byte[]: A binary representation of the given parameter
 * JSON: Any object not represented above is assumed to be a JSON deserialisation request
 * </pre>
 * 
 * Thus, for example, the signature
 * 
 * <pre>
 * String removeAllSpaces(String body)
 * </pre>
 * 
 * would receive the message body, as a string, as the function argument. The signature
 * 
 * <pre>
 * void withJsonObject(Square square)
 * </pre>
 * 
 * would attempt to parse a hypothetical <i>Square</i> class from the message body as interpreted as a UTF-8 string with
 * JSON contents.
 * <p>
 * <h2>PathParameters</h2>Without any other qualification, the argument will attempt to parse the message body – i.e.
 * {@link Message#getData()} - into the given type. With the {@link PathParam} annotation, we can direct the listener to
 * parse the argument from the string matched from the path. As before, all types supported in the message body are
 * supported from the path params, although the most common ones will be Number and String. For example:
 * 
 * <pre>
 * <code>
 * Path("/{floor}/{name}/room/{roomid: [0-9]*}")
 * void funcWithArgs(PathParam("floor") int floor, PathParam("name") String name, PathParam("roomid") int roomID)
 * </code>
 * </pre>
 * 
 * The above function will read out the parameters "floor", "name" and "roomid" and map them to the respective
 * arguments. Note that the path matching will be performed before type parsing, so the "floor" parameter may not be
 * parsed properly, as the path matching will match on any string, and not just an integer.
 * <p>
 * <h2>Return Values</h2> The parameter types are also accepted as return values to the function calls, with the
 * exception of the Resource type. If a known parameter is returned from a function called by a reflection listener, it
 * will either be sent immediately, or sent as a 200 OK response, with the content provided in the message body. If the
 * returned object is an unknown or {@link JSONSerializable} object, the content type will additionally be set to JSON.
 * The content type can be further overridden using the {@link Produces} annotation.
 * <p>
 * <h2>Exceptions</h2> Any exceptions thrown (except those discussed later) will be treated as 500 Internal Server
 * Errors, and reported as such. The remote side will receive an empty 500 error.
 * <p>
 * If a RestException is encountered, the status code will be used from the exception to determine the proper response.
 * RestExceptions can be used for a number of execution abortion causes, including 400 (client errors), 300 (redirects),
 * or custom abort routines.
 * 
 * @author Vladimir Katardjiev
 */
public class ReflectionListener
{
    
    private final Object       o;
    private final Method       m;
    private String             listenerPath;
    private LinkedList<String> matcherGroups = new LinkedList<String>();
    private String             regex;
    private Pattern            pattern;
    private String             dispatcherPath;
    private String             identifier;
    private RMBImpl            rmb;
    
    public ReflectionListener(RMBImpl rmb, Object o, Method m, String listenerPath)
    {
        
        this.rmb = rmb;
        // Generate the dispatcher path.
        // This is a constant string until the first variable element (i.e. { element)
        String[] parts = listenerPath.split("/");
        this.dispatcherPath = "";
        this.listenerPath = null;
        
        for (int i = 1; i < parts.length; i++)
        {
            this.dispatcherPath += "/";
            if (parts[i].startsWith("{"))
            {
                this.dispatcherPath += "**";
                this.listenerPath = listenerPath;
                break;
            }
            else if (parts[i].equals("**"))
            {
                this.dispatcherPath = listenerPath;
                this.listenerPath = null;
                break;
            }
            else
            {
                this.dispatcherPath += parts[i];
            }
        }
        
        this.o = o;
        this.m = m;
        
        // Create the listener regexp
        if (this.listenerPath != null)
        {
            this.regex = "";
            parts = this.listenerPath.split("/");
            for (int i = 1; i < parts.length; i++)
            {
                String part = parts[i];
                
                this.regex += "/";
                
                if (!part.startsWith("{"))
                {
                    this.regex += part;
                    continue;
                }
                
                // Find if the part has a custom regex
                int colonIdx = part.lastIndexOf(":");
                String partRegex = "([^/]*)";
                String partName = part.substring(1, part.length() - 1);
                
                if (colonIdx > -1)
                {
                    String[] regexBase = part.split(":");
                    partRegex = regexBase[1].trim();
                    partRegex = "(" + partRegex.substring(0, partRegex.length() - 1) + ")";
                    partName = regexBase[0].trim().substring(1);
                }
                
                this.regex += partRegex;
                this.matcherGroups.add(partName);
                
            }
            
            this.pattern = Pattern.compile(this.regex);
        }
        
        /*
         * For the reflection listener, we'll create a new identifier, a composite of object and method.
         * This composite will replace the hashCode of our ReflectionListener, making it apparently-identical
         * to any other ReflectionListener that uses the same object/method pair.
         */
        this.identifier = "ReflectionListener { " + o.getClass().getName() + " [" + o.hashCode() + "]" + " - " + m.toGenericString() + " }";
        
    }
    
    public String getDispatcherPath()
    {
        return this.dispatcherPath;
    }
    
    @Override
    public boolean equals(Object object)
    {
        if (!(object instanceof ReflectionListener))
            return false;
        
        ReflectionListener other = (ReflectionListener) object;
        
        return (this.o.equals(other.o) && this.m.equals(other.m));
        
    }
    
    public void receiveMessage(Message message)
    {
        
        try
        {
            HashMap<String, String> pathParams = null;
            
            if (this.pattern != null)
            {
                String path = message.to().toString();
                Matcher matcher = this.pattern.matcher(path);
                
                if (!matcher.matches())
                    return;
                
                pathParams = new HashMap<String, String>();
                
                int groupId = 1;
                for (String groupName : this.matcherGroups)
                {
                    String value = matcher.group(groupId);
                    pathParams.put(groupName, value);
                    groupId++;
                }
                
            }
            
            LinkedList<Object> objs = new LinkedList<Object>();
            Class<?>[] params = this.m.getParameterTypes();
            Annotation[][] annotations = this.m.getParameterAnnotations();
            
            for (int i = 0; i < params.length; i++)
            {
                Class<?> param = params[i];
                Annotation[] anns = annotations[i];
                
                String pathData = null;
                
                for (Annotation ann : anns)
                {
                    if (PathParam.class.isInstance(ann))
                    {
                        String pathElement = ((PathParam) ann).value();
                        pathData = pathParams.get(pathElement);
                    }
                    
                }
                
                if (param.isAssignableFrom(RMB.class))
                {
                    objs.add(rmb);
                }
                else if (param.isAssignableFrom(Message.class))
                {
                    objs.add(message);
                }
                else if (String.class.isAssignableFrom(param))
                {
                    objs.add(pathData != null ? pathData : message.string());
                }
                else if (Integer.class.isAssignableFrom(param) || Integer.TYPE.isAssignableFrom(param))
                {
                    objs.add(Integer.parseInt(pathData != null ? pathData : message.string()));
                }
                else if (Long.class.isAssignableFrom(param) || Long.TYPE.isAssignableFrom(param))
                {
                    objs.add(Long.parseLong(pathData != null ? pathData : message.string()));
                }
                else if (Double.class.isAssignableFrom(param) || Double.TYPE.isAssignableFrom(param))
                {
                    objs.add(Double.parseDouble(pathData != null ? pathData : message.string()));
                }
                else if (Float.class.isAssignableFrom(param) || Float.TYPE.isAssignableFrom(param))
                {
                    objs.add(Float.parseFloat(pathData != null ? pathData : message.string()));
                }
                else if (byte[].class.isAssignableFrom(param))
                {
                    objs.add(pathData != null ? pathData.getBytes() : message.bytes());
                }
                else
                {
                    objs.add(JSON.fromJSON(pathData != null ? pathData : message.string(), param));
                }
            }
            
            Object rv = this.m.invoke(this.o, objs.toArray());
            
            rmb.message().to(message.from()).data(rv).method("POST").status(200).send();
            
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IOException e)
        {
            e.printStackTrace();
        }
        
    }
}