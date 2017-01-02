package com.rashidmayes.clairvoyance;

import java.util.Map;

public class SetInfo implements Identifiable {
	public String namespace;
	public String name;
	public long objectCount;
	public long bytesMemory;
	public Map<String, String> properties;
	
	@Override
	public Object getId() {
		return "$set." + namespace + "."+ name;
	}
}
