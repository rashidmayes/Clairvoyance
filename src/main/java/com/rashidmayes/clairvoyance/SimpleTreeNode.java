package com.rashidmayes.clairvoyance;

import com.rashidmayes.clairvoyance.model.Identifiable;

public class SimpleTreeNode {
    String displayName;
    Identifiable value;

    public String toString() {
        return displayName;
    }

    public boolean equals(Object v) {
        return value.equals(v);
    }
}
