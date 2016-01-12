package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import io.paradoxical.cassieq.clustering.allocation.HazelcastResourceAllocater;
import io.paradoxical.cassieq.clustering.allocation.ResourceAllocator;

public class ResourceAllocationModule extends AbstractModule {
    /**
     * Configures a {@link Binder} via the exposed methods.
     */
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(ResourceAllocator.class, HazelcastResourceAllocater.class)
                        .build(ResourceAllocator.Factory.class));

    }
}
