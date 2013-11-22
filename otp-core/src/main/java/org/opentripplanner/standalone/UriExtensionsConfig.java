package org.opentripplanner.standalone;

import com.sun.jersey.api.core.PackagesResourceConfig;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

/** Registers URI extensions for some common media types.  This lets clients specify the desired
 * response format right in the URI like http://site.com/whatever.xml instead of
 * http://site.com/whatever with an Accept:application/xml header. */
public class UriExtensionsConfig extends PackagesResourceConfig
{
	private Map<String, MediaType> mediaTypeMap;

	public UriExtensionsConfig()
	{
		super("org.opentripplanner");
	}

	public UriExtensionsConfig(Map<String, Object> props)
	{
		super(props);
	}

	public UriExtensionsConfig(String[] paths)
	{
		super(paths);
	}

	public UriExtensionsConfig(String paths)
	{
		super(paths);
	}

	@Override
	public Map<String, MediaType> getMediaTypeMappings()
	{
		if (mediaTypeMap == null)
		{
			mediaTypeMap = new HashMap<String, MediaType>();
			mediaTypeMap.put("json", MediaType.APPLICATION_JSON_TYPE);
			mediaTypeMap.put("xml", MediaType.APPLICATION_XML_TYPE);
		}
		return mediaTypeMap;
	}
}
