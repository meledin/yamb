package us.yamb.rmb.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;

import us.yamb.mb.util.AnnotationScanner;
import us.yamb.rmb.annotations.DELETE;
import us.yamb.rmb.annotations.GET;
import us.yamb.rmb.annotations.HEAD;
import us.yamb.rmb.annotations.POST;
import us.yamb.rmb.annotations.PUT;
import us.yamb.rmb.annotations.Path;

/**
 * RestConverter allows the automatic generation of resource listeners based on annotations and reflection. This is
 * not a JSR311 implementation, but a similar (albeit much more limited) setup. This singleton depends on its
 * functionality for the Warp API already being running, and is so best called from a {@link WarpEnabled} instance. It
 * is an alternative to manually setting the message and/or method listeners.
 * <p>
 * To use the RestConverter, the class must be annotated with {@link Path}, and each method to be turned into a
 * listener must also be annotated with at least {@link Path}. Additionally, methods may be annotated with one of
 * {@link GET}, {@link PUT}, {@link POST} or {@link DELETE}. The path may include regular expressions (see
 * {@link ReflectionListener}), but each function call must have a unique path and method combination. This requirement
 * is not verified. If this requirement fails, one or more of the methods may not be called.
 * <pre>
 * 
 * &#064;Path(&quot;res&quot;)
 * class Example
 * {
 *     &#064;Path(&quot;foo&quot;)
 *     void onFooMessage(Message m)
 *     {
 *         // This method is called on ANY message to /res/foo
 *     }
 *     
 *     &#064;Path(&quot;rest&quot;)
 *     &#064;GET
 *     void onGet(Message m)
 *     {
 *         // This method is called on GET messages to /res/rest
 *     }
 *     
 *     &#064;Path(&quot;rest&quot;)
 *     &#064;GET
 *     void onPost(Message m)
 *     {
 *         // This method is called on POST messages to /res/rest
 *     }
 *     
 *     &#064;Path(&quot;rest&quot;)
 *     void onOther(Message m)
 *     {
 *         // This method is called on any non-GET or POST messages to /res/rest.
 *         // The requirement is basically it will receive any message not caught by a more specific listener
 *     }
 * }
 * 
 * </pre>
 * 
 * See {@link ReflectionListener} for further discussion on how reflected methods are called.
 * 
 * @author Vladimir Katardjiev
 */
public class RestConverter
{
    /**
     * Applies the convertion algorithm on all methods of the supplied <i>instance</i>. Uses reflection to acquire all
     * methods, using {@link Class#getMethods()}. Note this requires the methods to be <b>public</b>!
     * 
     * @param instance
     *            The instance to read
     * @param retain
     *            Whether to retain a reference to the instance. If retained, RestConverter guarantees it will not
     *            be garbage collected. Note that only one instance of each class will be retained.
     */
    public static void convert(RMBImpl target, Object instance)
    {
        convert(target, instance, "", null);
    }
    
    /**
     * Applies the convertion algorithm on all methods of the supplied <i>instance</i>. Uses reflection to acquire all
     * methods, using {@link Class#getMethods()}. Note this requires the methods to be <b>public</b>!
     * 
     * @param instance
     *            The instance to read
     * @param retain
     *            Whether to retain a reference to the instance. If retained, RestConverter guarantees it will not
     *            be garbage collected. Note that only one instance of each class will be retained.
     * @param basePath
     *            A path to prepend to the class base @Path() annotation. Useful when the class should have a virtual
     *            base, or is included by reference.
     */
    
    public static void convert(RMBImpl rmb, Object instance, String basePath, Object ctx)
    {
        Class<?> c = instance.getClass();
        
        HashMap<Class<? extends Annotation>, Annotation> anns = AnnotationScanner.scanClass(c);
        Path ann = (Path) anns.get(Path.class);
        
        String classPath = "";
        
        if (ann != null)
            classPath = processPathPart(ann.value());
        
        if (classPath.trim().length() > 0)
        {
            basePath += classPath + "/";
        }
        
        // We have the base path of the class, now let's start with the methods.
        
        Method[] methods = c.getMethods();
        
        for (Method m : methods)
        {
            
            anns = AnnotationScanner.scanMethod(c, m);
            
            String methodPath;
            try
            {
                ann = (Path) anns.get(Path.class);
                methodPath = ann.value();
            }
            catch (Exception e)
            {
                methodPath = "";
            }
            
            //Process the path
            methodPath = processPathPart(methodPath);
            
            String method = null;
            
            // Attempt to retrieve method, if applicable.
            // Classes are allowed to have no method, which makes them automatic listeners on all methods.
            
            try
            {
                if (anns.containsKey(GET.class))
                    method = "GET";
                
                if (anns.containsKey(PUT.class))
                    method = "PUT";
                
                if (anns.containsKey(POST.class))
                    method = "POST";
                
                if (anns.containsKey(DELETE.class))
                    method = "DELETE";
                
                if (anns.containsKey(HEAD.class))
                    method = "HEAD";
            }
            catch (Exception e)
            {
            }
            
            // We require at least one method
            if (method == null)
                continue;
            
            // Generate full path
            String listenerPath = basePath + methodPath;
            
            // Generate the dispatcher path.
            // This is a constant string until the first variable element (i.e. { element)
            ReflectionListener listener = new ReflectionListener(rmb, instance, m, listenerPath);
            listener.register(anns);
            
            /*
            String dispatcherPath = listener.getDispatcherPath().substring(1);
            
            RMB resource = dispatcherPath.trim().length() > 0 ? rmb.create(dispatcherPath) : rmb;
            
            try
            {
                if (m.getAnnotation(GET.class) != null)
                    resource.onget(msg -> listener.receiveMessage(msg));
                
                if (m.getAnnotation(PUT.class) != null)
                    resource.onput(msg -> listener.receiveMessage(msg));
                
                if (m.getAnnotation(POST.class) != null)
                    resource.onpost(msg -> listener.receiveMessage(msg));
                
                if (m.getAnnotation(DELETE.class) != null)
                    resource.ondelete(msg -> listener.receiveMessage(msg));
                
                if (m.getAnnotation(HEAD.class) != null)
                    resource.onhead(msg -> listener.receiveMessage(msg));
            }
            catch (Exception e)
            {
            }*/
            
        }
    }
    
    private static String processPathPart(String path)
    {
        
        if (path.startsWith("/"))
            path = path.substring(1);
        
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        
        return path;
    }
}
