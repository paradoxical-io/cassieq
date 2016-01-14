package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.configurations.AllocationConfig;
import io.paradoxical.cassieq.configurations.ClusteringConfig;
import io.paradoxical.cassieq.configurations.RepairConfig;

public class ConfigProviderModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    public RepairConfig getRepairConfig(ServiceConfiguration config) {
        return config.getRepairConf();
    }

    @Provides
    public ClusteringConfig getClusteringConfig(ServiceConfiguration config) {
        return config.getClusteringConfig();
    }

    @Provides
    public AllocationConfig getAllocationConfig(ServiceConfiguration config) {
        return config.getAllocationConfig();
    }
}
