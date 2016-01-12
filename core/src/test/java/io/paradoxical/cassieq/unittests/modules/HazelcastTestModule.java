package io.paradoxical.cassieq.unittests.modules;

import com.godaddy.logging.Logger;
import com.google.inject.Module;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.paradoxical.cassieq.modules.ClusteringModule;
import io.paradoxical.common.test.guice.OverridableModule;

import java.util.UUID;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class HazelcastTestModule extends OverridableModule {
    private HazelcastInstance instance;

    private static final Logger logger = getLogger(HazelcastTestModule.class);

    @Override
    public Class<? extends Module> getOverridesModule() {
        return ClusteringModule.class;
    }

    public synchronized HazelcastInstance getInstance() {
        if (instance == null) {
            Config config = new Config();
            config.setInstanceName(UUID.randomUUID().toString());
            config.setGroupConfig(new GroupConfig("cassieq", "cassieq"));
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
            config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1");
            config.getNetworkConfig().getInterfaces().clear();
            config.getNetworkConfig().getInterfaces().addInterface("127.0.0.*");
            config.getNetworkConfig().getInterfaces().setEnabled(true);

            // Heartbeat send interval in seconds
            config.setProperty("hazelcast.heartbeat.interval.seconds", "1");

            // Maximum timeout for heartbeat in seconds for a node to assume it is dead.
            config.setProperty("hazelcast.max.no.heartbeat.seconds", "1");

            // Interval at which master node publishes a member list
            config.setProperty("hazelcast.member.list.publish.interval.seconds", "1");

            //
            config.setProperty("Interval at which nodes send master confirmation.", "1");

            instance = Hazelcast.newHazelcastInstance(config);
        }

        return instance;
    }

    @Override
    protected void configure() {
        bind(HazelcastInstance.class).toInstance(getInstance());
    }

    @Override
    public void close() {
        if (instance != null) {
            instance.shutdown();

            logger.success("Shut down hazelcast");
        }
    }
}
