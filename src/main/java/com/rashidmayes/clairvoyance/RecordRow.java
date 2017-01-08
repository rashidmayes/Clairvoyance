package com.rashidmayes.clairvoyance;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import com.aerospike.client.Key;
import com.aerospike.client.Record;

public class RecordRow implements Runnable {
	
	public static final Record NULL_RECORD = new Record(new HashMap<String,Object>(),0,0);
	public static final Record LOADING_RECORD = new Record(new HashMap<String,Object>(),0,0);
	
	int index;
	Key key;
	private SoftReference<Record> referent;
	
	public RecordRow(Key key, Record record) {
		this.key = key;
		if ( record != null ) referent = new SoftReference<Record>(record);
	}
	
	public Record getRecord() {
		Record record = null;
		if ( referent == null || ((record = referent.get()) == null) ) {
			//App.EXECUTOR.execute(this);
			run();
			//record = LOADING_RECORD; // App.getClient().get(null, key);
			/*if ( record == null ) {
				record = NULL_RECORD;
			} else {
				referent = new SoftReference<Record>(record);
			}*/
		} 
		
		return record;	
	}
	
	public Key getKey() {
		return key;
	}
	
	public void run() {
		Record record = App.getClient().get(null, key);
		if ( record == null ) {
			record = NULL_RECORD;
		} /* else {
			referent = new SoftReference<Record>(record);
		}*/
		
		referent = new SoftReference<Record>(record);
	}
}
