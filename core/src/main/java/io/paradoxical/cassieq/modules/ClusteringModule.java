package io.paradoxical.cassieq.modules;

import com.godaddy.logging.Logger;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.paradoxical.cassieq.configurations.ClusteringConfig;

import java.io.File;
import java.io.FileNotFoundException;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class ClusteringModule extends AbstractModule {

    private static final Logger logger = getLogger(ClusteringModule.class);

    @Override
    protected void configure() {

    }

    @LazySingleton
    @Provides
    public HazelcastInstance getInstance(ClusteringConfig clusteringConfig) throws FileNotFoundException {
        if (new File(clusteringConfig.getOverridePath()).exists()) {
            logger.with("override-path", clusteringConfig.getOverridePath()).info("Using hazelcast override");

            final Config hazelcastXmlConfig = new XmlConfigBuilder(clusteringConfig.getOverridePath()).build();

            return Hazelcast.newHazelcastInstance(hazelcastXmlConfig);
        }

        return defaultConfig(clusteringConfig);
    }

    private HazelcastInstance defaultConfig(final ClusteringConfig clusteringConfig) {
        final Config hazelcastConfig = new Config();

        hazelcastConfig.setGroupConfig(new GroupConfig(clusteringConfig.getClusterName(), clusteringConfig.getClusterPassword()));

        final NetworkConfig networkConfig = new NetworkConfig();

        networkConfig.setPort(clusteringConfig.getMulticastPort());

        final JoinConfig joinConfig = new JoinConfig();

        final MulticastConfig multicastConfig = new MulticastConfig();

        multicastConfig.setMulticastPort(clusteringConfig.getMulticastPort());

        multicastConfig.setMulticastGroup(clusteringConfig.getMulticastGroup());

        joinConfig.setMulticastConfig(multicastConfig);

        networkConfig.setJoin(joinConfig);

        hazelcastConfig.setNetworkConfig(networkConfig);

        return Hazelcast.newHazelcastInstance(hazelcastConfig);
    }
}
