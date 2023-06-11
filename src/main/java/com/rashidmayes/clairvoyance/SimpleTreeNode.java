package com.rashidmayes.clairvoyance;

import com.rashidmayes.clairvoyance.model.Identifiable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTreeNode {

    public String displayName;
    public Identifiable value;

    public String toString() {
        return displayName;
    }

    public boolean equals(Object v) {
        return value.equals(v);
    }
}
