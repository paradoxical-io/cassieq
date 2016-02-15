package io.paradoxical.cassieq.configurations;

import io.dropwizard.util.Duration;
import lombok.Data;

@Data
public class AuthConfig {

    private Duration allowedClockSkew = Duration.seconds(5);
}
