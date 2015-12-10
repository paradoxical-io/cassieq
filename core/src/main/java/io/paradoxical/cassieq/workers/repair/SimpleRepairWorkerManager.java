package io.paradoxical.cassieq.workers.repair;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import io.paradoxical.cassieq.model.QueueDefinition;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Data
class RepairWorkerKey {
    private QueueDefinition getQueueDefinition() {
        return repairWorker.forDefinition();
    }

    private final RepairWorker repairWorker;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepairWorkerKey)) {
            return false;
        }

        final RepairWorkerKey that = (RepairWorkerKey) o;

        return !(getQueueDefinition() != null ? !getQueueDefinition().equals(that.getQueueDefinition()) : that.getQueueDefinition() != null);

    }

    @Override
    public int hashCode() {
        return getQueueDefinition() != null ? getQueueDefinition().hashCode() : 0;
    }
}

public class SimpleRepairWorkerManager implements RepairWorkerManager {
    private final RepairWorkerFactory repairWorkerFactory;
    private final Provider<QueueRepository> queueRepositoryProvider;
    private Set<RepairWorkerKey> currentRepairWorkers = new HashSet<>();

    @Inject
    public SimpleRepairWorkerManager(Provider<QueueRepository> queueRepositoryProvider, RepairWorkerFactory repairWorkerFactory) {
        this.queueRepositoryProvider = queueRepositoryProvider;
        this.repairWorkerFactory = repairWorkerFactory;
    }

    @Override
    public synchronized void start() {
        final Set<RepairWorkerKey> expectedWorkers =
                queueRepositoryProvider.get()
                                       .getQueues()
                                       .stream()
                                       .map(repairWorkerFactory::forQueue)
                                       .map(RepairWorkerKey::new)
                                       .collect(toSet());

        final ImmutableSet<RepairWorkerKey> itemsToStop = Sets.difference(expectedWorkers, currentRepairWorkers).immutableCopy();

        final ImmutableSet<RepairWorkerKey> newWorkers = Sets.difference(currentRepairWorkers, expectedWorkers).immutableCopy();

        itemsToStop.forEach(this::stop);

        currentRepairWorkers.removeAll(itemsToStop);

        currentRepairWorkers.addAll(newWorkers);

        newWorkers.forEach(this::start);
    }

    @Override
    public synchronized void stop() {
        if (!CollectionUtils.isEmpty(currentRepairWorkers)) {
            currentRepairWorkers.forEach(this::stop);

            currentRepairWorkers.clear();
        }
    }

    private void start(final RepairWorkerKey repairWorkerKey) {
        repairWorkerKey.getRepairWorker().start();
    }

    private void stop(final RepairWorkerKey repairWorkerKey) {
        repairWorkerKey.getRepairWorker().stop();
    }
}
