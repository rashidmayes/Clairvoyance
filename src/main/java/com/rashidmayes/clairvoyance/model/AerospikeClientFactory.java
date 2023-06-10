package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.async.AsyncClientPolicy;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.Result;

import java.util.logging.Level;

public class AerospikeClientFactory {

    public Result<AsyncClient, String> create(ConnectionInfo connectionInfo) {
        try {
            var policy = createPolicy(connectionInfo);
            var client = new AsyncClient(policy, connectionInfo.host(), connectionInfo.port());
            setDefaultConnectionParameters(client);
            ClairvoyanceLogger.logger.log(Level.INFO, "created new aerospike client");
            return Result.of(client);
        } catch (AerospikeException exception) {
            ClairvoyanceLogger.logger.log(Level.SEVERE, "could not create aerospike client", exception);
            return Result.error("could not connect to cluster: " + exception.getMessage());
        }
    }

    public AsyncClientPolicy createPolicy(ConnectionInfo connectionInfo) {
        var policy = new AsyncClientPolicy();
        policy.useServicesAlternate = connectionInfo.useServicesAlternate();
        policy.user = connectionInfo.username();
        policy.password = connectionInfo.password();
        return policy;
    }

    public void setDefaultConnectionParameters(AsyncClient client) {
        client.writePolicyDefault.timeout = 4000;
        client.readPolicyDefault.timeout = 4000;
        client.queryPolicyDefault.timeout = Integer.MAX_VALUE;
    }

}
