package io.paradoxical.cassieq.modules;

import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.workers.BucketConfiguration;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class ConfigProviderModule extends AbstractModule {
    @Override protected void configure() {

    }

    @Provides
    public BucketConfiguration bucketConfig(ServiceConfiguration config) {
        return config.getBucketConfiguration();
    }
}
