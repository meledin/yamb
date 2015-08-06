package us.yamb.rmb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.ericsson.research.trap.utils.LRUCache;

public class Location
{
    
    public static final int ROOT_ID = 1;
    
    /**
     * The path query, if any.
     */
    public final String query;
    
    /**
     * The full path.
     */
    public final String path;
    
    /**
     * An array of all the parts in the path. A part is an element of the path as delimited by slashes. In the path
     * 
     * <pre>
     * /foo/bar
     * </pre>
     * 
     * the three parts are ["", "foo", "bar"]. Index zero always represents the root as an empty string; relative paths will
     * have a non-empty string at index zero.
     */
    public final String[] parts;
    Map<String, String>   queryParams = null;
    
    private Location(String path, String query)
    {
        
        this.path = processed.get(path, () -> processResource(path));
        this.query = query;
        
        parts = processedParts.get(path, () -> {
            if (!"/".equals(path))
                return this.path.split("/");
            else
                return new String[] { "" };
        });
    }
    
    static final LRUCache<String, Location> locationCache = LRUCache.createCache(10000);
    
    public static Location parse(String str)
    {
        Location loc = locationCache.get(str);
        
        if (loc == null)
        {
            loc = new Location(str);
            locationCache.put(str, loc);
            
        }
        return loc;
        //return new Location(str);
    }
    
    private Location(String src)
    {
        int idx = src.indexOf("?");
        
        String path;
        String query;
        
        if (idx == -1)
        {
            query = null;
            path = src;
        }
        else
        {
            
            if (idx < src.length())
                query = src.substring(idx + 1);
            else
                query = "";
            path = src.substring(0, idx);
        }
        
        this.path = processed.get(path, () -> processResource(path));
        this.query = query;
        
        parts = processedParts.get(path, () -> {
            if (!"/".equals(path))
                return this.path.split("/");
            else
                return new String[] { "" };
        });
        
    }
    
    public String getPart(int idx)
    {
        return parts[idx];
    }
    
    static final LRUCache<String, String>   processed      = LRUCache.createCache(1000);
    static final LRUCache<String, String[]> processedParts = LRUCache.createCache(1000);
    
    private static String processResource(String resource)
    {
        if (resource == null)
            resource = "/";
        if (resource.indexOf("..") > -1)
        {
            StringTokenizer st = new StringTokenizer(resource, "/", true);
            List<String> pathParts = new ArrayList<String>(st.countTokens() >> (1 + 1));
            boolean slash = false;
            while (st.hasMoreElements())
            {
                String part = st.nextToken();
                if ("/".equals(part))
                {
                    if (slash)
                        pathParts.add("");
                    slash = true;
                    continue;
                }
                slash = false;
                if ("..".equals(part))
                {
                    if (pathParts.size() > 0)
                        pathParts.remove(pathParts.size() - 1);
                }
                else
                    pathParts.add(part);
            }
            StringBuffer sb = new StringBuffer(resource.length());
            for (Iterator<String> it = pathParts.iterator(); it.hasNext();)
            {
                String part = it.next();
                if (part != null)
                {
                    sb.append("/");
                    sb.append(part);
                }
            }
            if (sb.length() == 0)
                sb.append("/");
            resource = sb.toString();
        }
        else
        {
            if ((resource.length() == 0) || (resource.charAt(0) != '/'))
                resource = "/" + resource;
            if ((resource.length() > 1) && (resource.charAt(resource.length() - 1) == '/'))
                resource = resource.substring(0, resource.length() - 1);
        }
        return resource;
    }
    
    public Location withPath(String src)
    {
        String newPath = path + (path.endsWith("/") ? "" : "/") + src;
        return new Location(newPath, query);
    }
    
    /**
     * Inserts the part <b>src</b> at the index <b>idx</b>
     * 
     * @param src
     * @param idx
     * @return
     */
    public Location withPart(String src, int idx)
    {
        String[] tmp = Arrays.copyOf(parts, Math.max(idx + 1, parts.length));
        tmp[idx] = src;
        return new Location(String.join("/", tmp), query);
    }
    
    public Location withQuery(String src)
    {
        String q = query;
        
        if (q != null && q.trim().length() > 0)
            q += "&";
        else
            q = "";
            
        q += src;
        
        return new Location(path, q);
    }
    
    public Location withParameter(String name, String value)
    {
        Map<String, String> params = getParameters();
        
        StringBuilder sb = new StringBuilder();
        params.put(name, value);
        
        params.forEach((key, val) -> {
            sb.append(key);
            sb.append("=");
            sb.append(val);
            sb.append("&");
        });
        String str = sb.toString();
        return new Location(path, str.substring(0, str.length() - 1));
    }
    
    public String toString()
    {
        return path + (query != null ? "?" + query : "");
    }
    
    /**
     * Returns a map containing the Location query parameters.
     * 
     * @return A map, the keys of which are parameter names and the values are their values.
     */
    public Map<String, String> getParameters()
    {
        if (this.queryParams == null)
        {
            if (this.query != null)
            {
                Map<String, String> parameters = new HashMap<String, String>();
                StringTokenizer st = new StringTokenizer(this.query, "&");
                while (st.hasMoreElements())
                {
                    StringTokenizer st2 = new StringTokenizer(st.nextToken(), "=");
                    if (st2.hasMoreTokens())
                        parameters.put(st2.nextToken(), st2.hasMoreTokens() ? st2.nextToken() : "");
                }
                this.queryParams = parameters;
            }
            else
            {
                this.queryParams = new HashMap<String, String>();
            }
        }
        return this.queryParams;
    }
}
