package io.paradoxical.cassieq.configurations;

import lombok.Data;

@Data
public class ClusteringConfig {
    private boolean enabled = true;

    private int lockWaitSeconds = 5;

    private String clusterPassword = "cassieq";

    private String clusterName = "cassieq";

    private String multicastGroup = "224.2.2.3";

    private int multicastPort = 54327;

    /**
     * If the override path exists, will use the hazelcast settings from here
     */
    private String overridePath = "/data/conf/cluster/hazelcast.config";
}
