package io.paradoxical.cassieq.workers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class SimpleRepairWorkerManager implements RepairWorkerManager {
    private final RepairWorkerFactory repairWorkerFactory;
    private final Provider<QueueRepository> queueRepositoryProvider;
    private Set<RepairWorker> activeRepairWorkers = new HashSet<>();

    @Inject
    public SimpleRepairWorkerManager(Provider<QueueRepository> queueRepositoryProvider, RepairWorkerFactory repairWorkerFactory) {
        this.queueRepositoryProvider = queueRepositoryProvider;
        this.repairWorkerFactory = repairWorkerFactory;
    }

    @Override public synchronized void start() {
        final Set<RepairWorker> expectedWorkers =
                queueRepositoryProvider.get()
                                       .getQueues()
                                       .stream()
                                       .map(repairWorkerFactory::forQueue)
                                       .collect(toSet());

        for (RepairWorker worker : expectedWorkers) {
            if (!activeRepairWorkers.contains(worker)) {
                worker.start();

                activeRepairWorkers.add(worker);
            }
        }
    }

    @Override public synchronized void stop() {
        if (!CollectionUtils.isEmpty(activeRepairWorkers)) {
            activeRepairWorkers.forEach(RepairWorker::stop);

            activeRepairWorkers.clear();
        }
    }
}
