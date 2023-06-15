package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Objects;

@EqualsAndHashCode(of = "key")
public class RecordRow {

    public static final Record NULL_RECORD = new Record(new HashMap<>(), 0, 0);
    public static final Record LOADING_RECORD = new Record(new HashMap<>(), 0, 0);

    @Getter
    @Setter
    private int index;
    private final Key key;
    private SoftReference<Record> referent;

    public RecordRow(Key key, Record record) {
        this.key = key;
        if (record != null) {
            referent = new SoftReference<>(record);
        }
    }

    public Record getRecord() {
        // TODO: 12/06/2023 it fetches record from server and in case of collection it will operate over and over again
        if (referent == null || referent.get() == null) {
            ClairvoyanceLogger.logger.info("fetching record from server...");
            var client = ClairvoyanceFxApplication.getClient();
            var record = client.get(null, key);
            referent = new SoftReference<>(Objects.requireNonNullElse(record, NULL_RECORD));
        }
        return referent.get();
    }

    public Key getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "RecordRow{" +
                "index=" + index +
                ", key=" + key +
                ", referent=" + referent +
                '}';
    }
}
