package io.paradoxical.cassieq.modules;

import io.paradoxical.cassieq.dataAccess.AccountRepositoryImpl;
import io.paradoxical.cassieq.dataAccess.MessageRepositoryImpl;
import io.paradoxical.cassieq.dataAccess.MonotonicRepoImpl;
import io.paradoxical.cassieq.dataAccess.PointerRepositoryImpl;
import io.paradoxical.cassieq.dataAccess.QueueRepositoryImpl;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.DataContextFactoryImpl;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.PointerRepoFactory;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import io.paradoxical.cassieq.factories.QueueRepositoryFactory;

public class DataAccessModule extends AbstractModule {

    @Override protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(MessageRepository.class, MessageRepositoryImpl.class)
                        .build(MessageRepoFactory.class));

        install(new FactoryModuleBuilder()
                        .implement(PointerRepository.class, PointerRepositoryImpl.class)
                        .build(PointerRepoFactory.class));

        install(new FactoryModuleBuilder()
                        .implement(MonotonicRepository.class, MonotonicRepoImpl.class)
                        .build(MonotonicRepoFactory.class));


        install(new FactoryModuleBuilder()
                        .implement(QueueRepository.class, QueueRepositoryImpl.class)
                        .build(QueueRepositoryFactory.class));

        bind(AccountRepository.class).to(AccountRepositoryImpl.class);

        bind(DataContextFactory.class).to(DataContextFactoryImpl.class);
    }
}
