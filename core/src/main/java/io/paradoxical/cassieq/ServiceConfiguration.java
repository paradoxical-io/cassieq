package io.paradoxical.cassieq;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.paradoxical.cassieq.configurations.JerseyConfiguration;
import io.dropwizard.Configuration;
import io.paradoxical.cassieq.configurations.RepairConfig;
import io.paradoxical.cassieq.configurations.ServerConfig;
import io.paradoxical.cassieq.workers.BucketConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.stuartgunter.dropwizard.cassandra.CassandraFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ServiceConfiguration extends Configuration {
    @Getter
    @JsonProperty("jersey")
    @Setter
    private JerseyConfiguration jerseyConfiguration = new JerseyConfiguration();

    @Getter
    @Setter
    @JsonProperty("bucket")
    private BucketConfiguration bucketConfiguration = new BucketConfiguration();

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
    @JsonProperty("server")
    @Getter
    @Setter
    private ServerConfig serverConf = new ServerConfig();
}
