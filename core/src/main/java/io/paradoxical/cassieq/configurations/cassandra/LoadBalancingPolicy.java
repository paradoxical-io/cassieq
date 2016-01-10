package io.paradoxical.cassieq.configurations.cassandra;

import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class LoadBalancingPolicy {

    @NotNull
    @JsonProperty
    private String dataCenter;

    public TokenAwarePolicy build() {
        return new TokenAwarePolicy(new DCAwareRoundRobinPolicy(dataCenter));
    }

}
