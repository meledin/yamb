package us.yamb.rmb.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.mb.util.AnnotationScanner;
import us.yamb.mb.util.JSON;
import us.yamb.mb.util.JSONSerializable;
import us.yamb.mb.util.StringUtil;
import us.yamb.mb.util.YContext;
import us.yamb.rmb.Message;
import us.yamb.rmb.RMB;
import us.yamb.rmb.Response;
import us.yamb.rmb.Response.ResponseException;
import us.yamb.rmb.annotations.DELETE;
import us.yamb.rmb.annotations.GET;
import us.yamb.rmb.annotations.HEAD;
import us.yamb.rmb.annotations.JsonParam;
import us.yamb.rmb.annotations.POST;
import us.yamb.rmb.annotations.PUT;
import us.yamb.rmb.annotations.PathParam;
import us.yamb.rmb.annotations.Produces;
import us.yamb.rmb.annotations.QueryParam;
import us.yamb.rmb.kpi.CounterMonitor;
import us.yamb.rmb.kpi.TimeMonitor;
import us.yamb.rmb.kpi.TimeMonitor.Stopwatch;

/**
 * ReflectionListener is a wrapping class that allows objects to listen to messages at specific paths, with arbitrary method
 * names, and parsed parameters in the function call, in a manner similar to JSR311. For example, the following methods can be
 * called from a ReflectionListener:
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
 * Many more permutations are possible. Reflection listeners allow a variable <b>path</b>, parsing and reflection insertion of
 * <b>arguments</b>, arbitrary <b>return values</b> and handling of <b>exceptions</b>. We will handle each case in turn below.
 * <p>
 * <h2>Reflected Path</h2> We use a JSR311-like path for matching purposes. A reflection listener will handle parsing the path,
 * inserting PathParameters as applicable, as well as parsing the arguments to the correct type. Here are some acceptable paths:
 * 
 * <pre>
 * /foo/bar
 * /foo/{param}/bar
 * /foo/{param: [a-zA-Z]*}/bar
 * </pre>
 * 
 * Reflections will handle plain paths. Any parameter named {param} will be assumed to be a regular parameter, consisting of
 * regular URI-permitted characters. A regular expression can be specified using the syntax {param_name: regexp}; the pattern
 * matched by this regular expression will be bound to the named parameter.
 * <p>
 * <h2>Argument Insertion</h2> The ReflectionListener will automatically attempt to map relevant values to function argument
 * names, using the most relevant representation possible. The listener supports the automatic parsing and mapping of the
 * following types:
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
 * would attempt to parse a hypothetical <i>Square</i> class from the message body as interpreted as a UTF-8 string with JSON
 * contents.
 * <p>
 * <h2>PathParameters</h2>Without any other qualification, the argument will attempt to parse the message body – i.e.
 * {@link Message#getData()} - into the given type. With the {@link PathParam} annotation, we can direct the listener to parse
 * the argument from the string matched from the path. As before, all types supported in the message body are supported from the
 * path params, although the most common ones will be Number and String. For example:
 * 
 * <pre>
 * <code>
 * Path("/{floor}/{name}/room/{roomid: [0-9]*}")
 * void funcWithArgs(PathParam("floor") int floor, PathParam("name") String name, PathParam("roomid") int roomID)
 * </code>
 * </pre>
 * 
 * The above function will read out the parameters "floor", "name" and "roomid" and map them to the respective arguments. Note
 * that the path matching will be performed before type parsing, so the "floor" parameter may not be parsed properly, as the
 * path matching will match on any string, and not just an integer.
 * <p>
 * <h2>Return Values</h2> The parameter types are also accepted as return values to the function calls, with the exception of
 * the Resource type. If a known parameter is returned from a function called by a reflection listener, it will either be sent
 * immediately, or sent as a 200 OK response, with the content provided in the message body. If the returned object is an
 * unknown or {@link JSONSerializable} object, the content type will additionally be set to JSON. The content type can be
 * further overridden using the {@link Produces} annotation.
 * <p>
 * <h2>Exceptions</h2> Any exceptions thrown (except those discussed later) will be treated as 500 Internal Server Errors, and
 * reported as such. The remote side will receive an empty 500 error.
 * <p>
 * If a RestException is encountered, the status code will be used from the exception to determine the proper response.
 * RestExceptions can be used for a number of execution abortion causes, including 400 (client errors), 300 (redirects), or
 * custom abort routines.
 * 
 * @author Vladimir Katardjiev
 */
public class ReflectionListener
{
    
    private final Object                                      o;
    private final Method                                      m;
    private String                                            listenerPath;
    private LinkedList<String>                                matcherGroups = new LinkedList<String>();
    private String                                            regex;
    private Pattern                                           pattern;
    @SuppressWarnings("unused")
    private String                                            identifier;
    private RMB                                               rmb;
    
    static Map<Class<?>, BiFunction<String, Message, Object>> providers     = new HashMap<Class<?>, BiFunction<String, Message, Object>>();
    
    private CounterMonitor                                    numRequests;
    private CounterMonitor                                    numOKs;
    private CounterMonitor                                    numErrors;
    private TimeMonitor                                       servicingTime;
    private Object                                            ctx           = null;
    private Logger                                            logger        = LoggerFactory.getLogger(ReflectionListener.class);
    
    public ReflectionListener(RMBImpl rmb, Object o, Method m, String listenerPath)
    {
        this(rmb, o, m, listenerPath, null);
    }
    
    public ReflectionListener(RMBImpl rmb, Object o, Method m, String listenerPath, Object ctx)
    {
        this.ctx = ctx;
        
        String name = o.getClass().getSimpleName();
        if (name == null)
            name = o.getClass().getName();
        if (name == null)
            name = "anonymous";
        
        name += "_";
        name += m.getName();
        
        numRequests = MonitorFactory.counter(name + "_requests");
        numErrors = MonitorFactory.counter(name + "_err");
        numOKs = MonitorFactory.counter(name + "_ok");
        servicingTime = MonitorFactory.timer(name + "_time");
        
        MonitorFactory.register(this, name);
        
        this.rmb = rmb;
        String[] parts;
        this.listenerPath = listenerPath;
        
        this.o = o;
        this.m = m;
        
        this.regex = "";
        parts = this.listenerPath.split("/");
        
        if (parts.length == 1)
        {
            if ("".equals(parts[0]))
            {
                // do nothing
            }
            else
            {
                this.rmb = rmb.create(listenerPath);
                this.regex += "/" + listenerPath;
            }
        }
        else
        {
            for (int i = 0; i < parts.length; i++)
            {
                String part = parts[i];
                
                this.regex += "/";
                
                if (!part.startsWith("{"))
                {
                    
                    this.rmb = this.rmb.create(part);
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
                
                this.rmb = this.rmb.create(partRegex, true);
                
                this.regex += partRegex;
                this.matcherGroups.add(partName);
                
            }
        }
        
        /*
         * For the reflection listener, we'll create a new identifier, a composite of object and method.
         * This composite will replace the hashCode of our ReflectionListener, making it apparently-identical
         * to any other ReflectionListener that uses the same object/method pair.
         */
        this.identifier = "ReflectionListener { " + o.getClass().getName() + " [" + o.hashCode() + "]" + " - " + m.toGenericString() + " }";
        
    }
    
    @Override
    public boolean equals(Object object)
    {
        if (!(object instanceof ReflectionListener))
            return false;
        
        ReflectionListener other = (ReflectionListener) object;
        
        return (this.o.equals(other.o) && this.m.equals(other.m));
        
    }
    
    @SuppressWarnings("unchecked")
    public void receiveMessage(Message message)
    {
        
        if (logger.isTraceEnabled())
            logger.trace("Received message from {} to method {}.{}", message.to(), m.getDeclaringClass().getSimpleName(), m.getName());
        
        numRequests.increment();
        Stopwatch watch = servicingTime.start();
        try
        {
            YContext.push(RMB.CTX_MESSAGE, message);
            YContext.push(RMB.CTX_RMB, rmb);
            YContext.push(RMB.CTX_OBJECT, ctx);
            
            HashMap<String, String> pathParams = null;
            
            if (this.pattern == null)
            {
                this.pattern = Pattern.compile(rmb.id());
            }
            
            if (this.pattern != null)
            {
                String path = message.to().path;
                Matcher matcher = this.pattern.matcher(path);
                
                //System.out.println("Reflecting [" + pattern + " from " + path + " to " + path + "] " + matcher.matches());
                
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
            Parameter[] params = this.m.getParameters();
            Map<String, String> parameters = message.to().getParameters();
            Map<String, Object> body = null;
            
            for (int i = 0; i < params.length; i++)
            {
                Parameter param = params[i];
                Class<?> type = param.getType();
                
                HashMap<Class<? extends Annotation>, Annotation> anns = AnnotationScanner.scanParameter(o.getClass(), m, param);
                //Annotation[] anns = annotations[i];
                
                String pathData = null;
                PathParam pathAnn = (PathParam) anns.get(PathParam.class);
                QueryParam queryAnn = (QueryParam) anns.get(QueryParam.class);
                JsonParam jsonAnn = (JsonParam) anns.get(JsonParam.class);
                
                Function<String, String> getFieldName = (value) -> {
                    if (value.trim().length() > 0)
                        return value;
                    return param.getName();
                };
                
                if (pathAnn != null)
                {
                    String pathElement = getFieldName.apply(pathAnn.value());
                    pathData = pathParams.get(pathElement);
                }
                else if (queryAnn != null)
                {
                    String queryElement = getFieldName.apply(queryAnn.name());
                    pathData = parameters.get(queryElement);
                    if (pathData == null || pathData.trim().length() == 0)
                        pathData = queryAnn.value();
                }
                else if (jsonAnn != null)
                {
                    try
                    {
                        String path = getFieldName.apply(jsonAnn.value());
                        String[] keys = path.split("\\.");
                        Object cData = body;
                        
                        if (body == null)
                            body = message.object(Map.class);
                        
                        for (int k = 0; k < keys.length; k++)
                        {
                            cData = ((Map<String, Object>) cData).get(keys[k]);
                        }
                        pathData = cData.toString();
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException("Error parsing parameter " + param + " from JSON body");
                    }
                    
                }
                
                if (type.isAssignableFrom(RMB.class))
                {
                    objs.add(rmb);
                }
                else if (type.isAssignableFrom(Message.class))
                {
                    objs.add(message);
                }
                else if (providers.get(type) != null)
                {
                    BiFunction<String, Message, Object> provider = providers.get(type);
                    objs.add(provider.apply(pathData, message));
                }
                else
                {
                    String jsonStr = pathData != null ? pathData : message.string();
                    
                    if (jsonStr != null)
                        objs.add(JSON.fromJSON(jsonStr, type));
                    else
                        objs.add(null);
                }
            }
            
            Object rv;
            Response resp = null;
            try
            {
                rv = this.m.invoke(this.o, objs.toArray());
            }
            catch (Exception e)
            {
                rv = e;
            }
            
            // Check the tree for a ResponseException
            if (rv instanceof Throwable)
            {
                logger.trace("Execution yielded exception {}", rv.toString());
                Throwable e = (Throwable) rv;
                
                while (e instanceof InvocationTargetException || e instanceof UndeclaredThrowableException)
                {
                    
                    Throwable cause = e.getCause();
                    
                    if (cause == null)
                        cause = ((InvocationTargetException) e).getTargetException();
                    
                    if (cause == null)
                        break;
                    
                    e = cause;
                }
                
                while (!(e instanceof ResponseException) && e.getCause() != null && e.getCause() != e.getCause())
                    e = e.getCause();
                
                if (e instanceof ResponseException)
                    resp = ((ResponseException) e).response();
                
            }
            
            if (resp != null)
            {
                logger.trace("Sending message due to ResponseException; status is {}", resp);
                resp.to(message.from());
                resp.send(rmb);
                numOKs.increment();
            }
            else if (rv instanceof Response)
            {
                logger.trace("Sending message due to Response return value; status is {}", resp);
                ((Response) rv).to(message.from());
                ((Response) rv).send(rmb);
                numOKs.increment();
            }
            else if (rv instanceof Exception)
            {
                numErrors.increment();
                Exception e = (Exception) rv;
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                e.printStackTrace();
                Response.create().status(500).to(message).data(sw.toString()).send(rmb);
            }
            else if (rv instanceof AsyncResult<?>)
            {
                logger.trace("Neglecting to send a response due to AsyncResult");
                ((AsyncResult<?>) rv).setCallback((asyncResponse) -> {
                    try
                    {
                        Response.ok().to(message).data(asyncResponse).method("POST").send(rmb);
                        numOKs.increment();
                    }
                    catch (Exception e)
                    {
                        numErrors.increment();
                        throw new RuntimeException(e);
                    }
                }, (reason, ex) -> {
                    numErrors.increment();
                    throw new RuntimeException(ex);
                });
            }
            else if (rv != null)
            {
                logger.trace("Sending a 200 OK with data reply");
                Response.ok().to(message).status(200).data(rv).method("POST").send(rmb);
                numOKs.increment();
            }
            else if (!this.m.getReturnType().equals(void.class))
            {
                logger.trace("Sending a no-content reply");
                Response.ok().to(message).status(204).send(rmb);
                numOKs.increment();
            }
            else
            {
                logger.trace("Neglecting to send a response due to null result of void");
                numOKs.increment();
            }
            
        }
        catch (IllegalArgumentException | IOException e)
        {
            numErrors.increment();
            e.printStackTrace();
        }
        finally
        {
            YContext.pop(RMB.CTX_MESSAGE, message);
            YContext.pop(RMB.CTX_RMB, rmb);
            YContext.pop(RMB.CTX_OBJECT, ctx);
            watch.stop();
        }
        
    }
    
    public void register()
    {
        if (m.getAnnotation(GET.class) != null)
            rmb.onget(msg -> this.receiveMessage(msg));
        
        if (m.getAnnotation(PUT.class) != null)
            rmb.onput(msg -> this.receiveMessage(msg));
        
        if (m.getAnnotation(POST.class) != null)
            rmb.onpost(msg -> this.receiveMessage(msg));
        
        if (m.getAnnotation(DELETE.class) != null)
            rmb.ondelete(msg -> this.receiveMessage(msg));
        
        if (m.getAnnotation(HEAD.class) != null)
            rmb.onhead(msg -> this.receiveMessage(msg));
    }
    
    /**
     * Creates a Provider BiFunction that operates on bytes
     * 
     * @param fun
     *            A function capable of accepting a byte source, and returning an object.
     * @return A producer that can be plugged into ReflectionListener to produce the given output
     */
    public static BiFunction<String, Message, Object> byteProvider(Function<byte[], Object> fun)
    {
        
        return (string, msg) -> {
            byte[] src = null;
            if (string != null)
                src = StringUtil.toUtfBytes(string);
            else
                src = msg.bytes();
            
            return fun.apply(src);
        };
        
    }
    
    /**
     * Creates a Provider BiFunction that operates on Strings
     * 
     * @param fun
     *            A function capable of accepting a String source, and returning an object.
     * @return A producer that can be plugged into ReflectionListener to produce the given output
     */
    public static BiFunction<String, Message, Object> stringProvider(Function<String, Object> fun)
    {
        
        return (string, msg) -> {
            String src = null;
            if (string != null)
                src = string;
            else
            {
                if (msg.status() > 300)
                    Response.create().status(500).data(msg.string());
                src = msg.string();
            }
            
            return fun.apply(src);
        };
        
    }
    
    /**
     * Adds a provider that maps deserialization of objects.
     * 
     * @param provider
     *            A function that takes a String (may be null), and a {@link Message}, and returns the Object that is parsed
     *            from the string (if present), otherwise the message.
     * @param cls
     *            The classes to map this provider to.
     */
    public static void addProvider(BiFunction<String, Message, Object> provider, Class<?>... cls)
    {
        for (Class<?> c : cls)
            providers.put(c, provider);
    }
    
    static
    {
        // Init default providers
        addProvider(stringProvider(string -> string), String.class);
        addProvider(stringProvider(string -> Integer.parseInt(string)), Integer.class, Integer.TYPE);
        addProvider(stringProvider(string -> Long.parseLong(string)), Long.class, Long.TYPE);
        addProvider(stringProvider(string -> Double.parseDouble(string)), Double.class, Double.TYPE);
        addProvider(stringProvider(string -> Float.parseFloat(string)), Float.class, Float.TYPE);
        addProvider(stringProvider(string -> Boolean.parseBoolean(string)), Boolean.class, Boolean.TYPE);
        addProvider(stringProvider(string -> UUID.fromString(string)), UUID.class);
        addProvider(byteProvider(bs -> bs), byte[].class);
        
    }
    
    public void register(HashMap<Class<? extends Annotation>, Annotation> anns)
    {
        if (anns.containsKey(GET.class))
            rmb.onget(msg -> this.receiveMessage(msg));
        
        if (anns.containsKey(PUT.class))
            rmb.onput(msg -> this.receiveMessage(msg));
        
        if (anns.containsKey(POST.class))
            rmb.onpost(msg -> this.receiveMessage(msg));
        
        if (anns.containsKey(DELETE.class))
            rmb.ondelete(msg -> this.receiveMessage(msg));
        
        if (anns.containsKey(HEAD.class))
            rmb.onhead(msg -> this.receiveMessage(msg));
    }
    
    public static BiFunction<String, Message, Object> getProvider(Class<?> cls)
    {
        return providers.get(cls);
    }
    
}