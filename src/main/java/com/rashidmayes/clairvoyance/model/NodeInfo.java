package com.rashidmayes.clairvoyance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeInfo implements Identifiable {

    public String nodeId;
    public String build;
    public String edition;
    public String version;
    public String name;
    public String host;
    public String address;
    public List<NamespaceInfo> namespaces;

    public Map<String, String> statistics;

    @Override
    public String getId() {
        return "$node." + nodeId;
    }
}
