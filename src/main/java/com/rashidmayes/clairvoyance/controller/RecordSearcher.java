package com.rashidmayes.clairvoyance.controller;

import com.aerospike.client.Key;
import com.rashidmayes.clairvoyance.model.RecordRow;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RecordSearcher {

    private final List<RecordRow> searchResult = Collections.synchronizedList(new LinkedList<>());
    private volatile boolean activeSearch = false;

    public void search(Key key, List<RecordRow> allItems) {
        activeSearch = true;
        searchResult.clear();
        for (var recordRow : allItems) {
            if (recordRow.getKey().equals(key)) {
                searchResult.add(recordRow);
            }
        }
    }

    public List<RecordRow> getSearchResult() {
        return searchResult;
    }

    public boolean isActiveSearch() {
        return activeSearch;
    }

    public void reset() {
        activeSearch = false;
        searchResult.clear();
    }

}
