package com.rashidmayes.clairvoyance;

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
