package us.yamb.mb.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import flexjson.BeanAnalyzer;
import flexjson.BeanProperty;
import flexjson.ChainedSet;
import flexjson.JSONContext;
import flexjson.JSONDeserializer;
import flexjson.JSONException;
import flexjson.JSONSerializer;
import flexjson.JsonNumber;
import flexjson.ObjectBinder;
import flexjson.ObjectFactory;
import flexjson.Path;
import flexjson.TypeContext;
import flexjson.factories.BeanObjectFactory;
import flexjson.transformer.AbstractTransformer;
import flexjson.transformer.ObjectTransformer;
import flexjson.transformer.TransformerWrapper;

/**
 * The JSON class provides convenience methods for serialising and deserialising JSON objects into Java objects. The primitive
 * JSON objects can automatically be cast into Java object, and more advanced Java objects can be JSONified with no
 * configuration. <h2>JSON to/from Java primitives</h2> Java primitives can be directly serialised to JSON. The following
 * commands exemplify this:
 * 
 * <pre>
 * Map&lt;String, String&gt; values = JSON.fromJSON(&quot;{\&quot;foo\&quot;:\&quot;bar\&quot;}&quot;, Map.class);
 * JSON.toJSON(values); // Produces {&quot;foo&quot;:&quot;bar&quot;}
 * </pre>
 * 
 * <h2>JSON to/from Java Objects</h2> This utility class can transform Java objects with fields to/from their respective JSON
 * representations. So, for example, a Java object as so:
 * 
 * <pre>
 * class JSObj
 * {
 *     String foo = &quot;default&quot;;
 *     int    bar = 5;
 * }
 * </pre>
 * 
 * The class above can be used as so:
 * 
 * <pre>
 * JSON.toJSON(new JSObj()); // Produces {&quot;foo&quot;:&quot;default&quot;, &quot;bar&quot;:5}
 * JSObj o = JSON.fromJSON(&quot;...&quot;); // Will populate the fields foo and bar, trying to parse bar. Ignores all other fields
 * </pre>
 * 
 * Classes can be nested, so JSObj could contain a different Java class. These would also be serialised into JSON as applicable.
 * 
 * @author Vladimir Katardjiev
 * @since 2.0
 */
public class JSON
{
    
    static Map<Class<?>, AbstractTransformer> xformers  = new HashMap<Class<?>, AbstractTransformer>();
    static Map<Class<?>, ObjectFactory>       factories = new HashMap<Class<?>, ObjectFactory>();
    
    /**
     * Adds a JSON Transform, capable of mapping a Java class to/from JSON in a specified manner.
     * 
     * @param c
     *            The class to transform
     * @param xform
     *            A transformer, capable of serialising the object to JSON
     * @param factory
     *            A factory that can re-instantiate the object.
     */
    public static void addTransform(Class<?> c, AbstractTransformer xform)
    {
        xformers.put(c, xform);
    }
    
    /**
     * Adds a JSON Transform, capable of mapping a Java class to/from JSON in a specified manner.
     * 
     * @param c
     *            The class to transform
     * @param xform
     *            A transformer, capable of serialising the object to JSON
     * @param factory
     *            A factory that can re-instantiate the object.
     */
    public static void addTransform(Class<?> c, AbstractTransformer xform, ObjectFactory factory)
    {
        xformers.put(c, xform);
        factories.put(c, factory);
    }
    
    static
    {
        FieldObjectFactory f = new FieldObjectFactory();
        addTransform(JSONSerializable.class, new FieldTransformer(), f);
        addTransform(void.class, new ExcludeTransformer(), f);
        addTransform(UUID.class, new ToStringTransformer(), new UUIDFactory());
        addTransform(URI.class, new ToStringTransformer(), new URIFactory());

        try
        {
            JSON.addTransform(JsonNumber.class, new AbstractTransformer() {
                
                private Field field;
                {
                    field = JsonNumber.class.getDeclaredField("input");
                    field.setAccessible(true);
                }
                
                @Override
                public void transform(Object object)
                {
                    JSONContext ctx = JSONContext.get();
                    try
                    {
                        ctx.write((String) field.get(object));
                    }
                    catch (IllegalArgumentException | IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a JSONSerialiser with the transformers included by
     * {@link #addTransform(Class, AbstractTransformer, ObjectFactory)}.
     * 
     * @return
     */
    public static JSONSerializer serializer()
    {
        
        JSONSerializer s = new JSONSerializer();
        
        for (Entry<Class<?>, AbstractTransformer> e : xformers.entrySet())
        {
            s.transform(e.getValue(), e.getKey());
        }
        
        return s;
    }
    
    /**
     * Creates a JSONDeserializer with the transformers included by
     * {@link #addTransform(Class, AbstractTransformer, ObjectFactory)}.
     * 
     * @return
     */
    public static <T> JSONDeserializer<T> deserializer()
    {
        JSONDeserializer<T> d = new JSONDeserializer<T>();
        
        for (Entry<Class<?>, ObjectFactory> e : factories.entrySet())
        {
            d.use(e.getKey(), e.getValue());
        }
        
        d.use(Object.class, new FieldObjectFactory());
        
        return d;
    }
    
    /**
     * Serialises an object into JSON, returning the resulting string.
     * 
     * @param o
     *            The object to serialize. May be any Java object, including collections or custom objects. Must not include any
     *            reference loops.
     * @param excludes
     *            A list of fields to exclude from the serialization. This can be used to break loops. Fields are nested, so
     *            foo.bar.field is acceptable to exclude "field" in "bar" in "foo".
     * @return The JSON representation of the input, less the excludes.
     */
    public static String toJSON(Object o, String... excludes)
    {
        JSONSerializer s = serializer();
        
        // Let's be pretty for now...
        s.prettyPrint(true);
        s.exclude(excludes);
        
        return s.deepSerialize(o);
    }
    
    /**
     * Version of {@link #toJSON(Object, String...)} that outputs into a stream.
     * 
     * @param o
     *            The object to serialize
     * @param stream
     *            The stream to output into.
     */
    public static void toJSON(Object o, OutputStream stream)
    {
        JSONSerializer s = serializer();
        
        s.deepSerialize(o, new OutputStreamWriter(stream));
    }
    
    /**
     * Parses an input stream into an object.
     * 
     * @param is
     *            The stream to read from. Will assume the entire stream is ours to consume.
     * @param c
     *            The class to instantiate from the stream.
     * @return A new object as deserialized from the stream.
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromJSON(InputStream is, Class<T> c)
    {
        return (T) deserializer().deserialize(new InputStreamReader(is), c);
    }
    
    /**
     * Parses a string into an object.
     * 
     * @param s
     *            The string to read from. Assumes the entire string is a JSON object.
     * @param c
     *            The class to instantiate from the stream.
     * @return A new object as deserialized from the stream.
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromJSON(String s, Class<T> c)
    {
        if (s == null)
            return null;
        try
        {
            return (T) deserializer().deserialize(s, c);
        }
        catch (Exception e)
        {
            System.err.println(s);
            throw new IllegalArgumentException(s, e);
        }
    }
    
    /**
     * The ExcludeTransformer is a transformer that will exclude whatever item it represents from inclusion with the JSON. This
     * is useful for removing null values.
     * 
     * @author Vladimir Katardjiev
     */
    public static class ExcludeTransformer extends AbstractTransformer
    {
        
        public Boolean isInline()
        {
            return true;
        }
        
        public void transform(Object object)
        {
            // Do nothing, null objects are not serialized.
            return;
        }
    }
    
    /**
     * The ToStringTransformer will transform an object to JSON by calling its toString() method.
     * 
     * @author Vladimir Katardjiev
     */
    public static class ToStringTransformer extends AbstractTransformer
    {
        
        public void transform(Object object)
        {
            this.getContext().writeQuoted(object.toString());
        }
    }
    
}

class FieldTransformer extends ObjectTransformer
{
    
    @Override
    public void transform(Object object)
    {
        JSONContext context = this.getContext();
        Path path = context.getPath();
        ChainedSet visits = context.getVisits();
        try
        {
            if (!visits.contains(object))
            {
                context.setVisits(new ChainedSet(visits));
                context.getVisits().add(object);
                // traverse object
                TypeContext typeContext = context.writeOpenObject();
                Collection<String> fNames = ((JSONSerializable) object).getFieldNames();
                path.enqueue("class");
                if (context.isIncluded("class", object.getClass()))
                    this.xform("class", object.getClass(), context, typeContext);
                path.pop();
                for (String name : fNames)
                {
                    Field f = null;
                    Class<?> c = object.getClass();
                    do
                    {
                        try
                        {
                            f = c.getDeclaredField(name);
                            break;
                        }
                        catch (NoSuchFieldException e)
                        {
                            c = c.getSuperclass();
                        }
                    } while (object.getClass() != Object.class);
                    
                    if (f == null)
                        throw new NoSuchFieldException("Field [" + name + "] not found in " + object.getClass());
                    
                    path.enqueue(name);
                    f.setAccessible(true);
                    Object value = f.get(object);
                    this.xform(name, value, context, typeContext);
                    path.pop();
                }
                context.writeCloseObject();
                context.setVisits((ChainedSet) context.getVisits().getParent());
                
            }
        }
        catch (JSONException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new JSONException("Error trying to deepSerialize", e);
        }
    }
    
    private void xform(String name, Object value, JSONContext context, TypeContext typeContext) throws IllegalAccessException, InstantiationException
    {
        if (!context.getVisits().contains(value))
        {
            
            TransformerWrapper transformer = (TransformerWrapper) context.getTransformer(null, value);
            
            if (!transformer.isInline())
            {
                if (!typeContext.isFirst())
                    context.writeComma();
                else
                    typeContext.increment();
                context.writeName(name);
            }
            typeContext.setPropertyName(name);
            
            transformer.transform(value);
        }
    }
    
    @Override
    protected Class<? extends Object> resolveClass(Object obj)
    {
        return obj.getClass();
    }
    
}

class UUIDFactory extends BeanObjectFactory
{
    public Object instantiate(ObjectBinder context, Object value, Type targetType, @SuppressWarnings("rawtypes") Class targetClass)
    {
        return UUID.fromString(value.toString());
    }
}

class URIFactory extends BeanObjectFactory
{
    public Object instantiate(ObjectBinder context, Object value, Type targetType, @SuppressWarnings("rawtypes") Class targetClass)
    {
        return URI.create(value.toString());
    }
}

class FieldObjectFactory extends BeanObjectFactory
{
    
    @SuppressWarnings({ "unchecked", "rawtypes", "unused" })
    @Override
    public Object instantiate(ObjectBinder context, Object map, Type targetType, Class targetClass)
    {
        
        if (!JSONSerializable.class.isAssignableFrom(targetClass))
        {
            return super.instantiate(context, map, targetType, targetClass);
        }
        
        Path currentPath = null;
        Object target = null;
        try
        {
            
            if (!(map instanceof Map))
                throw new ClassCastException();
            
            // okay, I'm just going to reflect the **** out of this bastard
            Map<?, ?> jsonOwner = (Map<?, ?>) map;
            LinkedList<Object> objectStack = (LinkedList<Object>) this.getValue(context, "objectStack");
            LinkedList<Object> jsonStack = (LinkedList<Object>) this.getValue(context, "jsonStack");
            currentPath = (Path) this.getValue(context, "currentPath");
            Map<Class, ObjectFactory> factories = (Map<Class, ObjectFactory>) this.getValue(context, "factories");
            Map<Path, ObjectFactory> pathFactories = (Map<Path, ObjectFactory>) this.getValue(context, "pathFactories");
            
            Method findFieldInJson = context.getClass().getDeclaredMethod("findFieldInJson", Map.class, BeanProperty.class);
            findFieldInJson.setAccessible(true);
            Method resolveParameterizedTypes = context.getClass().getDeclaredMethod("resolveParameterizedTypes", Type.class, Type.class);
            resolveParameterizedTypes.setAccessible(true);
            Method bind = context.getClass().getDeclaredMethod("bind", Object.class, Type.class);
            bind.setAccessible(true);
            
            target = this.instantiate(targetClass);
            objectStack.add(target);
            LinkedList<BeanProperty> properties = new LinkedList<BeanProperty>();
            BeanAnalyzer ba = new FieldAnalyzer(targetClass);
            
            if (JSONSerializable.class.isAssignableFrom(target.getClass()))
            {
                Collection<String> fNames = ((JSONSerializable) target).getFieldNames();
                for (String fName : fNames)
                {
                    properties.add(new BeanProperty(fName, ba));
                }
            }
            for (BeanProperty descriptor : properties)
            {
                Object value = findFieldInJson.invoke(context, jsonOwner, descriptor);
                if (value != null)
                {
                    currentPath.enqueue(descriptor.getName());
                    Method setMethod = descriptor.getWriteMethod();
                    if (setMethod != null)
                    {
                        Type[] types = setMethod.getGenericParameterTypes();
                        if (types.length == 1)
                        {
                            Type paramType = types[0];
                            setMethod.invoke(objectStack.getLast(), bind.invoke(context, value, resolveParameterizedTypes.invoke(context, paramType, targetType)));
                        }
                        else
                        {
                            throw new JSONException(currentPath + ":  Expected a single parameter for method " + target.getClass().getName() + "." + setMethod.getName() + " but got " + types.length);
                        }
                    }
                    else
                    {
                        Field field = descriptor.getProperty();
                        if (field != null)
                        {
                            field.setAccessible(true);
                            field.set(target, bind.invoke(context, value, field.getGenericType()));
                        }
                    }
                    currentPath.pop();
                }
            }
            return objectStack.removeLast();
        }
        catch (IllegalAccessException e)
        {
            throw new JSONException(currentPath + ":  Could not access the no-arg constructor for " + target.getClass().getName(), e);
        }
        catch (InvocationTargetException ex)
        {
            throw new JSONException(currentPath + ":  Exception while trying to invoke setter method.", ex);
        }
        catch (Exception e)
        {
            throw new JSONException(e);
        }
    }
    
    private Object getValue(Object o, String value) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        Field f = o.getClass().getDeclaredField(value);
        f.setAccessible(true);
        return f.get(o);
    }
    
}

class FieldAnalyzer extends BeanAnalyzer
{
    
    protected FieldAnalyzer(Class<?> clazz)
    {
        super(clazz);
    }
    
}