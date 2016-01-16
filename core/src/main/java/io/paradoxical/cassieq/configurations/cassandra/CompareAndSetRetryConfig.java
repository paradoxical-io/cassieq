package io.paradoxical.cassieq.configurations.cassandra;

import lombok.Data;

@Data
public class CompareAndSetRetryConfig {
    private Boolean enabled = true;

    private int waitTimeMs = 25;

    private int maxWaitTimeMs = 250;

    private int maxRetries = 3;
}
