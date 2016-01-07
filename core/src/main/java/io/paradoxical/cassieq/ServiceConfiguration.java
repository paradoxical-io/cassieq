package io.paradoxical.cassieq;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.paradoxical.cassieq.configurations.ClusteringConfig;
import io.paradoxical.cassieq.configurations.LogConfig;
import io.paradoxical.cassieq.configurations.RepairConfig;
import lombok.Getter;
import lombok.Setter;
import org.stuartgunter.dropwizard.cassandra.CassandraFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ServiceConfiguration extends Configuration {
    @Valid
    @NotNull
    @JsonProperty("cassandra")
    @Getter
    @Setter
    private CassandraFactory cassandra = new CassandraFactory();

    @Valid
    @NotNull
    @JsonProperty("repair")
    @Getter
    @Setter
    private RepairConfig repairConf = new RepairConfig();

    @Valid
    @NotNull
    @JsonProperty("log")
    @Getter
    @Setter
    private LogConfig logConfig = new LogConfig();

    @Valid
    @NotNull
    @JsonProperty("clustering")
    @Getter
    @Setter
    private ClusteringConfig clusteringConfig = new ClusteringConfig();
}
