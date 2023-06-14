package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.IAerospikeClient;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AppStorage {

    private ConnectionInfo connectionInfo = null;
    private IAerospikeClient aerospikeClient = null;

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public ConnectionInfo getCurrentConnectionInfo() {
        return connectionInfo;
    }

    public IAerospikeClient getAerospikeClient() {
        return this.aerospikeClient;
    }

    public void setAerospikeClient(IAerospikeClient client) {
        this.aerospikeClient = client;
    }

}
