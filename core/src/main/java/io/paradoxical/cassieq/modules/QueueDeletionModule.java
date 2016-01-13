package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import io.paradoxical.cassieq.workers.QueueDeleter;

public class QueueDeletionModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(QueueDeleter.class, QueueDeleter.class)
                        .build(QueueDeleter.Factory.class));

    }
}
