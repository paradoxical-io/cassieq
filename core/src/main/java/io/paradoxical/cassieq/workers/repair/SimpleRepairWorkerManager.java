package io.paradoxical.cassieq.workers.repair;

import com.godaddy.logging.Logger;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toSet;

public class SimpleRepairWorkerManager implements RepairWorkerManager {
    private static final Logger logger = getLogger(SimpleRepairWorkerManager.class);

    private final RepairWorkerFactory repairWorkerFactory;
    private final Provider<QueueRepository> queueRepositoryProvider;
    @Getter
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
                                       .getActiveQueues()
                                       .stream()
                                       .map(repairWorkerFactory::forQueue)
                                       .map(RepairWorkerKey::new)
                                       .collect(toSet());

        final ImmutableSet<RepairWorkerKey> newWorkers = Sets.difference(expectedWorkers, currentRepairWorkers).immutableCopy();

        final ImmutableSet<RepairWorkerKey> itemsToStop = Sets.difference(currentRepairWorkers, expectedWorkers).immutableCopy();

        logger.with("count", itemsToStop.size()).info("Removing workers");
        logger.with("count", newWorkers.size()).info("Workers to add");

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
