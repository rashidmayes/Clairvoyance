package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.async.AsyncClient;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.Result;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.C;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationModel {

    public static final ApplicationModel INSTANCE = new ApplicationModel();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3, r -> {
        var thread = Executors.defaultThreadFactory()
                .newThread(r);
        thread.setDaemon(true);
        return thread;
    });

    private final AerospikeClientFactory aerospikeClientFactory = new AerospikeClientFactory();

    private final AppStorage appStorage = new AppStorage();

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        appStorage.setConnectionInfo(connectionInfo);
    }

    public ConnectionInfo getConnectionInfo() {
        return appStorage.getCurrentConnectionInfo();
    }

    public void runInBackground(Runnable action) {
        EXECUTOR.execute(action);
    }

    public <T> Future<T> runInBackground(Callable<T> callable) {
        return EXECUTOR.submit(callable);
    }

    public Result<AsyncClient, String> createNewAerospikeClient() {
        ClairvoyanceLogger.logger.log(Level.INFO, "creating new aerospike client");
        var clientResult = aerospikeClientFactory.create(appStorage.getCurrentConnectionInfo());
        if (clientResult.hasError()) {
            return clientResult;
        }
        appStorage.setAerospikeClient(clientResult.getData());
        return clientResult;
    }

    public Result<AsyncClient, String> getAerospikeClient() {
        var client = appStorage.getAerospikeClient();
        if (client == null || !client.isConnected()) {
            if (appStorage.getCurrentConnectionInfo() != null) {
                return createNewAerospikeClient();
            } else {
                return Result.error("cannot create new aerospike client - no connection info");
            }
        }
        ClairvoyanceLogger.logger.log(Level.INFO, "returning existing aerospike client");
        return Result.of(client);
    }

}
