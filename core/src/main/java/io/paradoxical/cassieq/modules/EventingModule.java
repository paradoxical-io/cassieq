package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.paradoxical.cassieq.clustering.eventing.EventBus;
import io.paradoxical.cassieq.clustering.eventing.HazelcastEventBus;

public class EventingModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(EventBus.class).to(HazelcastEventBus.class).in(LazySingleton.class);
    }
}
