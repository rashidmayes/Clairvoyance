package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.async.AsyncClient;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AppStorage {

    private ConnectionInfo connectionInfo = null;
    private AsyncClient aerospikeClient = null;

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public ConnectionInfo getCurrentConnectionInfo() {
        return connectionInfo;
    }

    public AsyncClient getAerospikeClient() {
        return this.aerospikeClient;
    }

    public void setAerospikeClient(AsyncClient client) {
        this.aerospikeClient = client;
    }

}
