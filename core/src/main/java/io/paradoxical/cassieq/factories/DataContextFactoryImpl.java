package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.QueueDefinition;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.paradoxical.cassieq.model.QueueName;

import java.util.Optional;

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
                monotonicRepoFactory.forQueue(definition),
                pointerRepoFactory.forQueue(definition));
    }

    @Override
    public Optional<QueueDefinition> getDefinition(final QueueName name) {
        return queueRepositoryProvider.get().getActiveQueue(name);
    }
}


