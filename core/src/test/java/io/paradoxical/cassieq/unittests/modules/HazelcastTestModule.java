package io.paradoxical.cassieq.unittests.modules;

import com.godaddy.logging.Logger;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.paradoxical.cassieq.modules.ClusteringModule;
import io.paradoxical.common.test.guice.OverridableModule;

import java.util.UUID;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class HazelcastTestModule extends OverridableModule {
    private final String clusterName;
    private HazelcastInstance instance;

    private static final Logger logger = getLogger(HazelcastTestModule.class);

    public HazelcastTestModule(String clusterName){
        this.clusterName = clusterName;
    }

    @Override
    public Class<? extends Module> getOverridesModule() {
        return ClusteringModule.class;
    }

    @Provides
    @LazySingleton
    public synchronized HazelcastInstance getInstance() {
        if (instance == null) {
            Config config = new Config();
            config.setInstanceName(clusterName + "_" + UUID.randomUUID().toString());
            config.setGroupConfig(new GroupConfig(clusterName));
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
            config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1");
            config.getNetworkConfig().getInterfaces().clear();
            config.getNetworkConfig().getInterfaces().addInterface("127.0.0.*");
            config.getNetworkConfig().getInterfaces().setEnabled(true);

            instance = Hazelcast.newHazelcastInstance(config);
        }

        return instance;
    }

    @Override
    protected void configure() {

    }

    @Override
    public void close() {
        if (instance != null) {
            instance.shutdown();

            logger.success("Shut down hazelcast");
        }
    }
}
