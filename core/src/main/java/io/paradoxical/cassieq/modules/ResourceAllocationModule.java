package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.clustering.allocation.EventableDispatchAllocater;
import io.paradoxical.cassieq.clustering.allocation.HazelcastResourceAllocater;
import io.paradoxical.cassieq.clustering.allocation.ManualResourceAllocater;
import io.paradoxical.cassieq.clustering.allocation.ResourceAllocator;
import io.paradoxical.cassieq.clustering.allocation.ResourceConfig;
import io.paradoxical.cassieq.clustering.allocation.ResourceIdentity;
import io.paradoxical.cassieq.configurations.AllocationConfig;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ResourceAllocationModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    public ResourceAllocator.Factory getResourceFactory(AllocationConfig config, Injector injector) {
        return new ResourceAllocatorStrategySelector(config, injector).getFactory();
    }

    class ResourceAllocatorStrategySelector {
        private final AllocationConfig config;
        private final Injector injector;

        @Inject
        public ResourceAllocatorStrategySelector(AllocationConfig config, Injector injector) {
            this.config = config;
            this.injector = injector;
        }

        public ResourceAllocator.Factory getFactory() {
            switch (config.getStrategy()) {
                case CLUSTER:
                    return (config, setSupplier, claimSetConsumer) -> {
                        final Injector childInjector = getChildInjector(config, setSupplier, claimSetConsumer);

                        // create a clustered allocator and wrap it with an eventable allocator
                        final ResourceAllocator clusteredResourceAllocator = childInjector.getInstance(HazelcastResourceAllocater.class);

                        return childInjector.createChildInjector(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(ResourceAllocator.class).toInstance(clusteredResourceAllocator);
                            }
                        }).getInstance(EventableDispatchAllocater.class);
                    };
                case NONE:
                    return (config, setSupplier, claimSetConsumer) -> passThroughResourceAllocator(setSupplier, claimSetConsumer);
                case MANUAL:
                    return (config, setSupplier, claimSetConsumer) -> getChildInjector(config, setSupplier, claimSetConsumer).getInstance(ManualResourceAllocater.class);
            }

            return null;
        }

        private ResourceAllocator passThroughResourceAllocator(
                final Supplier<Set<ResourceIdentity>> setSupplier,
                final Consumer<Set<ResourceIdentity>> claimSetConsumer) {
            return new ResourceAllocator() {
                @Override
                public void claim() {
                    claimSetConsumer.accept(setSupplier.get());
                }

                @Override
                public void close() throws Exception {

                }
            };
        }

        private Injector getChildInjector(
                final ResourceConfig config,
                final Supplier<Set<ResourceIdentity>> setSupplier,
                final Consumer<Set<ResourceIdentity>> claimSetConsumer) {

            final Injector childInjector = injector.createChildInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(new TypeLiteral<Supplier<Set<ResourceIdentity>>>() {}).annotatedWith(Assisted.class).toInstance(setSupplier);
                    bind(new TypeLiteral<Consumer<Set<ResourceIdentity>>>() {}).annotatedWith(Assisted.class).toInstance(claimSetConsumer);
                    bind(ResourceConfig.class).annotatedWith(Assisted.class).toInstance(config);
                }
            });
            return childInjector;
        }
    }
}
