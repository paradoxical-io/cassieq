package io.paradoxical.cassieq.clustering.allocation;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ResourceAllocator extends AutoCloseable {
    void claim();

    @FunctionalInterface
    interface Factory {
        ResourceAllocator getAllocator(ResourceConfig config,
                                       Supplier<Set<ResourceIdentity>> masterSetSupplier,
                                       final Consumer<Set<ResourceIdentity>> claimSetConsumer);
    }
}