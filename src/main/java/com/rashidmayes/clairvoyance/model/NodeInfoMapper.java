package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.Info;
import com.aerospike.client.cluster.Node;
import com.rashidmayes.clairvoyance.util.MapHelper;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class NodeInfoMapper {

    public NodeInfo getNodeInfo(Node node) {
        var details = Info.request(null, node);
        return NodeInfo.builder()
                .nodeId(details.get("node"))
                .build(details.get("build"))
                .edition(details.get("edition"))
                .version(details.get("version"))
                .statistics(MapHelper.map(details.get("statistics"), ";"))
                .address(node.getAddress().toString())
                .host(node.getHost().toString())
                .name(node.getName())
                .namespaces(getNamespaceInfo(node))
                .build();
    }

    public List<NamespaceInfo> getNamespaceInfo(Node node) {
        var namespaces = new ArrayList<NamespaceInfo>();
        var info = Info.request(null, node, "namespaces");
        for (String namespace : StringUtils.split(info, ";")) {
            var oneStringNamespaceInfo = Info.request(null, node, "namespace/" + namespace);
            var namespaceInfo = NamespaceInfo.builder()
                    .name(namespace)
                    .properties(MapHelper.map(oneStringNamespaceInfo, ";"))
                    .sets(getSetInfo(node, namespace))
                    .build();
            namespaces.add(namespaceInfo);
        }
        return namespaces;
    }

    public static List<SetInfo> getSetInfo(Node node, String namespace) {
        var sets = new ArrayList<SetInfo>();
        var oneStringSetInfo = Info.request(null, node, "sets/" + namespace);
        for (String set : StringUtils.split(oneStringSetInfo, ";")) {
            var map = MapHelper.map(set, ":");
            var setInfo = SetInfo.builder()
                    .namespace(MapHelper.getString(map, "ns_name", "ns"))
                    .name(MapHelper.getString(map, "set_name", "set"))
                    .objectCount(MapHelper.getLong(map, "n_objects", "objects"))
                    .bytesMemory(MapHelper.getLong(map, "n-bytes-memory", "memory_data_bytes"))
                    .properties(map)
                    .build();
            sets.add(setInfo);
        }
        return sets;
    }

}
