package io.paradoxical.cassieq;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.paradoxical.cassieq.configurations.AllocationConfig;
import io.paradoxical.cassieq.configurations.AuthConfig;
import io.paradoxical.cassieq.configurations.ClusteringConfig;
import io.paradoxical.cassieq.configurations.LogConfig;
import io.paradoxical.cassieq.configurations.RepairConfig;
import io.paradoxical.cassieq.configurations.cassandra.CassandraConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class ServiceConfiguration extends Configuration {
    @Valid
    @NotNull
    @JsonProperty("cassandra")
    private CassandraConfiguration cassandra = new CassandraConfiguration();

    @Valid
    @NotNull
    @JsonProperty("repair")
    private RepairConfig repairConf = new RepairConfig();

    @Valid
    @NotNull
    @JsonProperty("log")
    private LogConfig logConfig = new LogConfig();

    @Valid
    @NotNull
    @JsonProperty("clustering")
    private ClusteringConfig clusteringConfig = new ClusteringConfig();

    @Valid
    @NotNull
    @JsonProperty("allocation")
    private AllocationConfig allocationConfig = new AllocationConfig();

    @Valid
    @NotNull
    @JsonProperty("auth")
    private AuthConfig authConfig = new AuthConfig();
}
