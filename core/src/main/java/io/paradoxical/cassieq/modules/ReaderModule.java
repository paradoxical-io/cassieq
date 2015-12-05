package io.paradoxical.cassieq.modules;

import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.workers.reader.Reader;
import io.paradoxical.cassieq.workers.reader.ReaderImpl;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class ReaderModule extends AbstractModule {
    @Override protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(Reader.class, ReaderImpl.class)
                        .build(ReaderFactory.class));
    }
}

