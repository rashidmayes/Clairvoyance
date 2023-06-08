package com.rashidmayes.clairvoyance.model;

import com.rashidmayes.clairvoyance.util.MapHelper;

import java.util.HashMap;
import java.util.Map;

public class NamespaceInfo implements Identifiable {

    public String name;
    public Map<String, String> properties = new HashMap<String, String>();
    public String[] bins;
    public SetInfo[] sets;

    public long getObjects() {
        return MapHelper.getLong(properties, 0, "objects");
    }

    public String getType() {
        return MapHelper.getString(properties, "", "type", "storage-engine");
    }

    public long getProleObjects() {
        return MapHelper.getLong(properties, 0, "prole-objects", "prole_objects");
    }

    public long getUsedBytesMemory() {
        return MapHelper.getLong(properties, 0, "used-bytes-memory", "memory_used_bytes");
    }

    public long getReplicationFactor() {
        return MapHelper.getLong(properties, 0, "repl-factor");
    }

    public long getUsedBytesDisk() {
        return MapHelper.getLong(properties, 0, "used-bytes-disk", "device_used_bytes");
    }

    public long getMasterObjects() {
        return MapHelper.getLong(properties, 0, "master-objects", "master_objects");
    }

    public long getTotalBytesMemory() {
        return MapHelper.getLong(properties, 0, "total-bytes-memory", "memory-size");
    }

    public long getTotalBytesDisk() {
        return MapHelper.getLong(properties, 0, "total-bytes-disk", "device_total_bytes");
    }

    public long getFreeMemoryPercent() {
        return MapHelper.getLong(properties, 0, "free-pct-memory", "memory_free_pct");
    }

    public long getFreeDiskPercent() {
        return MapHelper.getLong(properties, 0, "free-pct-disk", "device_available_pct");
    }

    @Override
    public Object getId() {
        return "$namespace." + name;
    }
}
