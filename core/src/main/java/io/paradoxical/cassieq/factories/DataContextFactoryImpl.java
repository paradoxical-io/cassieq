package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.QueueDefinition;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class DataContextFactoryImpl implements DataContextFactory {
    private final MonotonicRepoFactory monotonicRepoFactory;
    private final PointerRepoFactory pointerRepoFactory;
    private final MessageRepoFactory messageRepoFactory;
    private final Provider<QueueRepository> queueRepositoryProvider;

    @Inject
    public DataContextFactoryImpl(
            MonotonicRepoFactory monotonicRepoFactory,
            PointerRepoFactory pointerRepoFactory,
            MessageRepoFactory messageRepoFactory,
            Provider<QueueRepository> queueRepositoryProvider) {
        this.monotonicRepoFactory = monotonicRepoFactory;
        this.pointerRepoFactory = pointerRepoFactory;
        this.messageRepoFactory = messageRepoFactory;
        this.queueRepositoryProvider = queueRepositoryProvider;
    }

    public DataContext forQueue(QueueDefinition definition) {
        return new DataContext(
                messageRepoFactory.forQueue(definition),
                monotonicRepoFactory.forQueue(definition.getQueueName()),
                pointerRepoFactory.forQueue(definition.getQueueName()),
                queueRepositoryProvider.get());
    }
}


