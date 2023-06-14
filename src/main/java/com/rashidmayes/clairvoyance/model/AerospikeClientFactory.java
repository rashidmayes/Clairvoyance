package com.rashidmayes.clairvoyance.model;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.EventLoop;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.async.NettyEventLoops;
import com.aerospike.client.policy.ClientPolicy;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.Result;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.*;

public class AerospikeClientFactory {

    private static final ExecutorService executor = new ThreadPoolExecutor(
            2, 4, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
            runnable -> {
                var thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName("event-loop-thread-" + thread.threadId());
                thread.setDaemon(true);
                return thread;
            }
    );

    private static final EventLoops eventLoops;

    static {
        var group = new NioEventLoopGroup(4, executor);
        eventLoops = new NettyEventLoops(group);
    }

    public Result<IAerospikeClient, String> create(ConnectionInfo connectionInfo) {
        try {
            var policy = createPolicy(connectionInfo);
            var client = new AerospikeClient(policy, connectionInfo.host(), connectionInfo.port());
            setDefaultConnectionParameters(client);
            ClairvoyanceLogger.logger.info("created new aerospike client");
            return Result.of(client);
        } catch (AerospikeException exception) {
            ClairvoyanceLogger.logger.error("could not create aerospike client", exception);
            return Result.error("could not connect to cluster: " + exception.getMessage());
        }
    }

    public ClientPolicy createPolicy(ConnectionInfo connectionInfo) {
        var policy = new ClientPolicy();
        policy.useServicesAlternate = connectionInfo.useServicesAlternate();
        policy.user = connectionInfo.username();
        policy.password = connectionInfo.password();
        policy.eventLoops = eventLoops;
        return policy;
    }

    public void setDefaultConnectionParameters(IAerospikeClient client) {
        client.getReadPolicyDefault().totalTimeout = 4000;
        client.getQueryPolicyDefault().totalTimeout = 40_000;
        ClairvoyanceLogger.logger.debug("read policy timeout set to {}", client.getReadPolicyDefault().totalTimeout);
        ClairvoyanceLogger.logger.debug("query policy timeout set to {}", client.getQueryPolicyDefault().totalTimeout);
    }

    public EventLoop createEventLoop() {
        var eventLoop = eventLoops.next();
        ClairvoyanceLogger.logger.info("got event loop with index {}", eventLoop);
        return eventLoop;
    }

}
