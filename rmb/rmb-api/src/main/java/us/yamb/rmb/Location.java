package us.yamb.rmb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class Location
{
    
    public static final int ROOT_ID = 1;
    
	/**
	 * The path query, if any.
	 */
	public final String	  query;

	/**
	 * The full path.
	 */
	public final String	  path;

	/**
	 * An array of all the parts in the path. A part is an element of the path
	 * as delimited by slashes. In the path
	 * 
	 * <pre>
	 * /foo/bar
	 * </pre>
	 * 
	 * the three parts are ["", "foo", "bar"]. Index zero always represents the
	 * root as an empty string; relative paths will have a non-empty string at
	 * index zero.
	 */
	public final String[]	parts;
	Map<String, String>	  queryParams	= null;
	
	private Location(String path, String query)
	{

		this.path = processResource(path);
		this.query = query;
		
		if (!"/".equals(path))
			parts = this.path.split("/");
		else
			parts = new String[] { "" };
	}

	public Location(String src)
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

		this.path = processResource(path);
		this.query = query;
		
		if (!"/".equals(path))
			parts = this.path.split("/");
		else
			parts = new String[] { "" };

	}
	
	public String getPart(int idx)
	{
		return parts[idx];
	}
	
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
    
    public String toString() {
        return path + (query != null ? "?"+query : "");
    }

    
    /**
     * Returns a map containing the Location query parameters.
     * 
     * @return A map, the keys of which are parameter names and the values are their values.
     */
    public Map<String, String> getParameters()
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
            return parameters;
        }
        return null;
    }
}
