package io.paradoxical.cassieq.configurations.cassandra;

import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import org.stuartgunter.dropwizard.cassandra.pooling.HostDistanceOptions;
import org.stuartgunter.dropwizard.cassandra.pooling.PoolingOptionsFactory;

import javax.validation.Valid;

public class PoolingOptionsV3 extends PoolingOptionsFactory {

    @Valid
    private Duration heartbeatInterval;
    @Valid
    private Duration poolTimeout;
    @Valid
    private HostDistanceOptions remote;
    @Valid
    private HostDistanceOptions local;

    @JsonProperty
    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    @JsonProperty
    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    @JsonProperty
    public Duration getPoolTimeout() {
        return poolTimeout;
    }

    @JsonProperty
    public void setPoolTimeout(Duration poolTimeout) {
        this.poolTimeout = poolTimeout;
    }

    @JsonProperty
    public HostDistanceOptions getRemote() {
        return remote;
    }

    @JsonProperty
    public void setRemote(HostDistanceOptions remote) {
        this.remote = remote;
    }

    @JsonProperty
    public HostDistanceOptions getLocal() {
        return local;
    }

    @JsonProperty
    public void setLocal(HostDistanceOptions local) {
        this.local = local;
    }

    public PoolingOptions build() {
        PoolingOptions poolingOptions = new PoolingOptions();
        if (local != null) {
            setPoolingOptions(poolingOptions, HostDistance.LOCAL, local);
        }
        if (remote != null) {
            setPoolingOptions(poolingOptions, HostDistance.REMOTE, remote);
        }
        if (heartbeatInterval != null) {
            poolingOptions.setHeartbeatIntervalSeconds((int) heartbeatInterval.toSeconds());
        }
        if (poolTimeout != null) {
            poolingOptions.setPoolTimeoutMillis((int) poolTimeout.toMilliseconds());
        }
        return poolingOptions;
    }

    private void setPoolingOptions(PoolingOptions poolingOptions, HostDistance hostDistance, HostDistanceOptions options) {
        if (options.getCoreConnections() != null) {
            poolingOptions.setCoreConnectionsPerHost(hostDistance, options.getCoreConnections());
        }
        if (options.getMaxConnections() != null) {
            poolingOptions.setMaxConnectionsPerHost(hostDistance, options.getMaxConnections());
        }
        if (options.getMaxSimultaneousRequests() != null) {
            poolingOptions.setMaxConnectionsPerHost(hostDistance, options.getMaxSimultaneousRequests());
        }
    }
}
