package io.paradoxical.cassieq.workers;

import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class SimpleRepairWorkerManager implements RepairWorkerManager {
    private final RepairWorkerFactory repairWorkerFactory;
    private final Provider<QueueRepository> queueRepositoryProvider;
    private List<RepairWorker> activeRepairWorkers;

    @Inject
    public SimpleRepairWorkerManager(Provider<QueueRepository> queueRepositoryProvider, RepairWorkerFactory repairWorkerFactory) {
        this.queueRepositoryProvider = queueRepositoryProvider;
        this.repairWorkerFactory = repairWorkerFactory;
    }

    @Override public void start() {
        activeRepairWorkers = queueRepositoryProvider.get().getQueues().stream().map(repairWorkerFactory::forQueue).collect(toList());

        activeRepairWorkers.forEach(RepairWorker::start);
    }

    @Override public void stop() {
        if (!CollectionUtils.isEmpty(activeRepairWorkers)) {
            activeRepairWorkers.forEach(RepairWorker::stop);
        }
    }
}
