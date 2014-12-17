package us.yamb.rmb;

import java.util.Map;

public class Path
{
	final String	    query;
	final String	    path;
	final String[]	    pathParts;
	Map<String, String>	queryParams	= null;

	public Path(String path)
	{
		int idx = path.indexOf("?");

		if (idx == -1)
		{
			query = "";
			this.path = path;
		}
		else
		{

			if (idx < path.length())
				query = path.substring(idx + 1);
			else
				query = "";
			this.path = path.substring(0, idx);
		}

		if (!"/".equals(path))
			pathParts = this.path.split("/");
		else
			pathParts = new String[] { "" };

	}

}
