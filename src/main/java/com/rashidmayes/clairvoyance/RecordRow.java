package com.rashidmayes.clairvoyance;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import lombok.Getter;
import lombok.Setter;

import java.lang.ref.SoftReference;
import java.util.HashMap;

public class RecordRow implements Runnable {

    public static final Record NULL_RECORD = new Record(new HashMap<>(), 0, 0);
    public static final Record LOADING_RECORD = new Record(new HashMap<>(), 0, 0);

    @Getter
    @Setter
    private int index;
    private Key key;
    private SoftReference<Record> referent;

    public RecordRow(Key key, Record record) {
        this.key = key;
        if (record != null) {
            referent = new SoftReference<>(record);
        }
    }

    public Record getRecord() {
        Record record = null;
        if (referent == null || ((record = referent.get()) == null)) {
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

    @Override
    public void run() {
        Record record = ClairvoyanceFxApplication.getClient().get(null, key);
        if (record == null) {
            record = NULL_RECORD;
        } /* else {
			referent = new SoftReference<Record>(record);
		}*/

        referent = new SoftReference<Record>(record);
    }
}
