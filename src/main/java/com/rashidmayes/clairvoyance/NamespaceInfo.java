package com.rashidmayes.clairvoyance;

import java.util.HashMap;
import java.util.Map;

import com.rashidmayes.clairvoyance.util.MapHelper;

public class NamespaceInfo implements Identifiable {
	
	public String name;
	public Map<String, String> properties  = new HashMap<String, String>();		
	public String[] bins;
	public SetInfo[] sets;
	
	public long getObjects() {
		return MapHelper.getLong(properties, 0, "objects");
	}
	
	public String getType() {
		return MapHelper.getString(properties, "", "type");
	}
	
	public long getProleObjects() {
		return MapHelper.getLong(properties, 0, "prole-objects");
	}
	
	public long getUsedBytesMemory() {
		return MapHelper.getLong(properties, 0, "used-bytes-memory");
	}
	
	public long getReplicationFactor() {
		return MapHelper.getLong(properties, 0, "repl-factor");
	}
	
	public long getUsedBytesDisk() {
		return MapHelper.getLong(properties, 0, "used-bytes-disk");
	}
	
	public long getMasterObjects() {
		return MapHelper.getLong(properties, 0, "master-objects");
	}
	
	public long getTotalBytesMemory() {
		return MapHelper.getLong(properties, 0, "total-bytes-memory");
	}
	
	public long getTotalBytesDisk() {
		return MapHelper.getLong(properties, 0, "total-bytes-disk");
	}
	
	public long getFreeMemoryPercent() {
		return MapHelper.getLong(properties, 0, "free-pct-memory");
	}
	
	public long getFreeDiskPercent() {
		return MapHelper.getLong(properties, 0, "free-pct-disk");
	}
	
	@Override
	public Object getId() {
		return "$namespace." + name;
	}
}
