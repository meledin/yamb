package us.yamb.mb.util;

import java.util.Collection;

public interface JSONSerializable
{
	/**
	 * This method must be implemented by any JSONSerializable, and it must
	 * return the complete list of field names that should be serialized when
	 * the object is serialized into JSON.
	 * 
	 * @return
	 */
	public Collection<String> getFieldNames();
}
