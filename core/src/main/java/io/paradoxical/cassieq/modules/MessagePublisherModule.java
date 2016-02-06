package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import io.paradoxical.cassieq.workers.DefaultMessagePublisher;
import io.paradoxical.cassieq.workers.MessagePublisher;

public class MessagePublisherModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(MessagePublisher.class).to(DefaultMessagePublisher.class);
    }
}
