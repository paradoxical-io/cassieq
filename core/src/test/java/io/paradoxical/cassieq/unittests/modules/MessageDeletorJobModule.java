package io.paradoxical.cassieq.unittests.modules;

import com.google.inject.Module;
import io.paradoxical.cassieq.factories.MessageDeleterJobProcessorFactory;
import io.paradoxical.cassieq.modules.MessageDeletionModule;
import io.paradoxical.common.test.guice.OverridableModule;

public class MessageDeletorJobModule extends OverridableModule {
    private final MessageDeleterJobProcessorFactory processor;

    public MessageDeletorJobModule(MessageDeleterJobProcessorFactory processor) {
        this.processor = processor;
    }

    @Override
    public Class<? extends Module> getOverridesModule() {
        return MessageDeletionModule.class;
    }

    @Override
    protected void configure() {
        bind(MessageDeleterJobProcessorFactory.class).toInstance(processor);
    }
}
