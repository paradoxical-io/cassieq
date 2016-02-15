package io.paradoxical.cassieq.configurations;

public enum AllocationStrategy {
    /**
     * Cluster with Hazelcast
     */
    CLUSTER,

    /**
     * All workers duplicate work
     */
    NONE,

    /**
     * Manually assign instance id's and total members for sharding
     */
    MANUAL
}
