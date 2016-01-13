package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.QueueDefinition;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;

import java.util.Optional;

public class DataContextFactoryImpl implements DataContextFactory {
    private final MonotonicRepoFactory monotonicRepoFactory;
    private final PointerRepoFactory pointerRepoFactory;
    private final MessageRepoFactory messageRepoFactory;
    private final QueueRepositoryFactory queueRepositoryFactory;
    private final Provider<AccountRepository> accountRepositoryProvider;

    @Inject
    public DataContextFactoryImpl(
            MonotonicRepoFactory monotonicRepoFactory,
            PointerRepoFactory pointerRepoFactory,
            MessageRepoFactory messageRepoFactory,
            QueueRepositoryFactory queueRepositoryFactory,
            Provider<AccountRepository> accountRepositoryProvider) {
        this.monotonicRepoFactory = monotonicRepoFactory;
        this.pointerRepoFactory = pointerRepoFactory;
        this.messageRepoFactory = messageRepoFactory;
        this.queueRepositoryFactory = queueRepositoryFactory;
        this.accountRepositoryProvider = accountRepositoryProvider;
    }

    public QueueDataContext forQueue(QueueDefinition definition) {
        return new QueueDataContext(
                messageRepoFactory.forQueue(definition),
                monotonicRepoFactory.forQueue(definition.getId()),
                pointerRepoFactory.forQueue(definition.getId()));
    }

    @Override
    public QueueRepository forAccount(final AccountName accountName) {
        return queueRepositoryFactory.forAccount(accountName);
    }

    @Override
    public AccountRepository getAccountRepository() {
        return accountRepositoryProvider.get();
    }

    @Override
    public MonotonicRepository getMonotonicRepository(QueueId queueId){
        return monotonicRepoFactory.forQueue(queueId);
    }

    @Override
    public PointerRepository getPointerRepository(QueueId queueId){
        return pointerRepoFactory.forQueue(queueId);
    }
}


