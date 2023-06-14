package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.IAerospikeClient;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.Result;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.concurrent.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationModel {

    public static final ApplicationModel INSTANCE = new ApplicationModel();

    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(10),
            runnable -> {
                var thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName("application-background-task-thread-" + thread.threadId());
                thread.setDaemon(true);
                return thread;
            });

    @Getter
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

    public Result<IAerospikeClient, String> createNewAerospikeClient() {
        ClairvoyanceLogger.logger.info("creating new aerospike client");
        var clientResult = aerospikeClientFactory.create(appStorage.getCurrentConnectionInfo());
        if (clientResult.hasError()) {
            return clientResult;
        }
        appStorage.setAerospikeClient(clientResult.getData());
        return clientResult;
    }

    public Result<IAerospikeClient, String> getAerospikeClient() {
        var client = appStorage.getAerospikeClient();
        if (client == null || !client.isConnected()) {
            if (appStorage.getCurrentConnectionInfo() != null) {
                return createNewAerospikeClient();
            } else {
                return Result.error("cannot create new aerospike client - no connection info");
            }
        }
        ClairvoyanceLogger.logger.info("returning existing aerospike client");
        return Result.of(client);
    }

}
