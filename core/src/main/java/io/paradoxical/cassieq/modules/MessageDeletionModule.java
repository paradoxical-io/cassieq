package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import io.paradoxical.cassieq.dataAccess.MessageDeletorJobProcessorImpl;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageDeleterJobProcessor;
import io.paradoxical.cassieq.factories.MessageDeleterJobProcessorFactory;

public class MessageDeletionModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(MessageDeleterJobProcessor.class, MessageDeletorJobProcessorImpl.class)
                        .build(MessageDeleterJobProcessorFactory.class));

    }
}
