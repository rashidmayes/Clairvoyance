package com.rashidmayes.clairvoyance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetInfo implements Identifiable {

    public String namespace;
    public String name;
    public long objectCount;
    public long bytesMemory;
    public Map<String, String> properties;

    @Override
    public String getId() {
        return "$set." + namespace + "." + name;
    }

}
