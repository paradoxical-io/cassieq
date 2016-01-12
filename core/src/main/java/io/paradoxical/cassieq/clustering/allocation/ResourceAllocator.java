package io.paradoxical.cassieq.clustering.allocation;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ResourceAllocator {
    void claim();

    interface Factory {
        ResourceAllocator getAllocator(ResourceConfig config, Supplier<Set<ResourceIdentity>> inputSet, final Consumer<Set<ResourceIdentity>> whenAllocated);
    }
}
