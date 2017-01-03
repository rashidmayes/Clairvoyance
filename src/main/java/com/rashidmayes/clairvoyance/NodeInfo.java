package com.rashidmayes.clairvoyance;

import java.util.Map;

public class NodeInfo implements Identifiable {
	public String nodeId;
	public String build;
	public String edition;
	public String version;
	public String name;
	public String host;
	public String address;
	public NamespaceInfo[] namespaces;
	
	public Map<String,String> statistics;
	
	@Override
	public Object getId() {
		return "$node." + nodeId;
	}
}
