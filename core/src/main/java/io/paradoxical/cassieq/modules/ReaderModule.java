package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import io.paradoxical.cassieq.factories.InvisLocaterFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.workers.reader.InvisLocator;
import io.paradoxical.cassieq.workers.reader.InvisLocatorImpl;
import io.paradoxical.cassieq.workers.reader.Reader;
import io.paradoxical.cassieq.workers.reader.ReaderImpl;

public class ReaderModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(Reader.class, ReaderImpl.class)
                        .build(ReaderFactory.class));

        install(new FactoryModuleBuilder()
                        .implement(InvisLocator.class, InvisLocatorImpl.class)
                        .build(InvisLocaterFactory.class));
    }
}

