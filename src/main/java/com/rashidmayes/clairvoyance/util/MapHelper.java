package com.rashidmayes.clairvoyance.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class MapHelper {

	public static Map<String, String> map(String in, String delim) {
		Map<String, String> map = new HashMap<String, String>();
		String[] kvPair;
		for ( String pair : StringUtils.split(in,delim) ) {
			kvPair = pair.split("=");
			map.put(kvPair[0], kvPair[1]);
		}
		
		return map;
	}	
	
	public static String getValue(Map<?,?> map, String... keys) {
		if ( map == null ) {
			return null;
		} else {
			Object value = null;
			for (String key : keys) {
				value = map.get(key);
				if ( value != null ) {
					break;
				}
			}
			
			return (value == null) ? null : value.toString();
		}
	}
	
	public static String getProperty(Map<String, String> properties, String... keys) {
		if ( properties != null ) {
			String value;
			for (String key : keys) {
				value = properties.get(key);
				if ( value != null) {
					return value;
				}
			}
		}

		return null;
	}
	
	
	public static String getString(Map<String, String> properties, String def, String... keys) {
		String value = getProperty(properties, keys);
		return ( value == null ) ? def : value;
	}
	
	public static long getLong(Map<String, String> properties, long def, String... keys) {
		String value = getProperty(properties, keys);
		if ( value != null ) {
			try {
				return Long.parseLong(value);
			} catch (NumberFormatException nfe) {
				
			}
		}
		
		return def;
	}
}
