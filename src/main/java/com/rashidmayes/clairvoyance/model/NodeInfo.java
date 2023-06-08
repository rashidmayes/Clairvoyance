package com.rashidmayes.clairvoyance.model;

import com.rashidmayes.clairvoyance.model.Identifiable;
import com.rashidmayes.clairvoyance.model.NamespaceInfo;

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

    public Map<String, String> statistics;

    @Override
    public Object getId() {
        return "$node." + nodeId;
    }
}
