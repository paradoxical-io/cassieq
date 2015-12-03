package io.paradoxical.cassieq.modules;

import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.workers.Reader;
import io.paradoxical.cassieq.workers.ReaderImpl;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class ReaderModule extends AbstractModule {
    @Override protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(Reader.class, ReaderImpl.class)
                        .build(ReaderFactory.class));
    }
}

