package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.workers.DefaultMessageConsumer;
import io.paradoxical.cassieq.workers.MessageConsumer;
import io.paradoxical.cassieq.workers.reader.InvisStrategy;
import io.paradoxical.cassieq.workers.reader.PointerBasedInvisStrategy;
import io.paradoxical.cassieq.workers.reader.Reader;
import io.paradoxical.cassieq.workers.reader.ReaderImpl;

public class ReaderModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(Reader.class, ReaderImpl.class)
                        .build(ReaderFactory.class));

        install(new FactoryModuleBuilder()
                        .implement(InvisStrategy.class, PointerBasedInvisStrategy.class)
                        .build(InvisStrategy.Factory.class));

        install(new FactoryModuleBuilder()
                        .implement(MessageConsumer.class, DefaultMessageConsumer.class)
                        .build(MessageConsumer.Factory.class));
    }
}

