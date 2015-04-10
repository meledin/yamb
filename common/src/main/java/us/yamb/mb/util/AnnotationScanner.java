package us.yamb.mb.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;

public class AnnotationScanner
{
    
    /**
     * Performs a deep scan of an object for its annotations. <b>This is an expensive operation</b> as we scan all parent
     * classes an interfaces.
     * 
     * @param cls
     *            The base class to use for the scanning process. All superclasses and implemented interfaces will be scanned
     *            for relevant annotations
     * @param method
     *            The method to scan.
     * @return
     */
    public static HashMap<Class<? extends Annotation>, Annotation> scanMethod(Class<?> baseClass, Method method)
    {
        BiConsumer<HashMap<Class<? extends Annotation>, Annotation>, Class<?>> methodScanner = (map, cls) -> {
            try
            {
                Method m = cls.getMethod(method.getName(), method.getParameterTypes());
                if (m != null)
                {
                    Annotation[] anns = m.getAnnotations();
                    for (Annotation ann : anns)
                        map.putIfAbsent(ann.annotationType(), ann);
                }
            }
            catch (NoSuchMethodException | SecurityException e)
            {
            }
        };
        return traverse(baseClass, methodScanner);
    }
    
    /**
     * Performs a deep scan of an object for its annotations. <b>This is an expensive operation</b> as we scan all parent
     * classes an interfaces.
     * 
     * @param cls
     *            The base class to use for the scanning process. All superclasses and implemented interfaces will be scanned
     *            for relevant annotations
     * @param method
     *            The method to scan.
     * @return
     */
    public static HashMap<Class<? extends Annotation>, Annotation> scanParameter(Class<?> baseClass, Method method, Parameter param)
    {
        BiConsumer<HashMap<Class<? extends Annotation>, Annotation>, Class<?>> methodScanner = (map, cls) -> {
            try
            {
                Method m = cls.getMethod(method.getName(), method.getParameterTypes());
                if (m != null)
                {
                    Parameter[] parameters = m.getParameters();
                    for (Parameter p : parameters)
                    {
                        if (p.getName().equals(param.getName()))
                        {
                            Annotation[] anns = p.getAnnotations();
                            for (Annotation ann : anns)
                                map.putIfAbsent(ann.annotationType(), ann);
                        }
                    }
                }
            }
            catch (NoSuchMethodException | SecurityException e)
            {
            }
        };
        return traverse(baseClass, methodScanner);
    }
    
    /**
     * Scans a class for an array of annotations.
     * 
     * @param baseClass
     *            The class to scan. All superclasses will be included
     *            @return A map of all annotations by type, in the order of encountered-first.
     */
    public static HashMap<Class<? extends Annotation>, Annotation> scanClass(Class<?> baseClass)
    {
        BiConsumer<HashMap<Class<? extends Annotation>, Annotation>, Class<?>> methodScanner = (map, cls) -> {
            Annotation[] anns = cls.getAnnotations();
            for (Annotation ann : anns)
                map.putIfAbsent(ann.annotationType(), ann);
        };
        return traverse(baseClass, methodScanner);
    }
    
    /**
     * Traverses the full parent hierarchy and returns all annotations according to the provided accumulator.
     * 
     * @param cls
     * @param accumulator
     * @return
     */
    static HashMap<Class<? extends Annotation>, Annotation> traverse(Class<?> cls, BiConsumer<HashMap<Class<? extends Annotation>, Annotation>, Class<?>> accumulator)
    {
        // Create a set of all possible parents.
        final HashSet<Class<?>> elts = new HashSet<Class<?>>();
        final HashMap<Class<? extends Annotation>, Annotation> anns = new HashMap<Class<? extends Annotation>, Annotation>();
        
        do
        {
            
            elts.add(cls);
            for (Class<?> iface : cls.getInterfaces())
                elts.add(iface);
            
            cls = cls.getSuperclass();
            
        } while (cls != Object.class);
        
        elts.forEach((c) -> accumulator.accept(anns, c));
        return anns;
    }
}
