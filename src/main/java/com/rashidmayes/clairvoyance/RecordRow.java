package com.rashidmayes.clairvoyance;

import java.lang.ref.SoftReference;

import com.aerospike.client.Key;
import com.aerospike.client.Record;

public class RecordRow {
	int index;
	Key key;
	private SoftReference<Record> referent;
	
	public RecordRow(Key key, Record record) {
		this.key = key;
		referent = new SoftReference<Record>(record);
	}
	
	public Record getRecord() {
		Record record = referent.get();
		if ( record == null ) {
			record = App.getClient().get(null, key);
			referent = new SoftReference<Record>(record);
		}
		
		return record;
	}
	
	public Key getKey() {
		return key;
	}
}
