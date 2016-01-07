package io.paradoxical.cassieq.workers.repair;

import com.godaddy.logging.Logger;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.paradoxical.cassieq.configurations.RepairConfig;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import io.paradoxical.cassieq.modules.annotations.GenericScheduler;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toSet;

public class SimpleRepairWorkerManager implements RepairWorkerManager {
    private static final Logger logger = getLogger(SimpleRepairWorkerManager.class);

    private final RepairWorkerFactory repairWorkerFactory;
    private final RepairConfig config;
    private final ScheduledExecutorService scheduler;
    private final Provider<QueueRepository> queueRepositoryProvider;
    private ScheduledFuture<?> cancellationToken;
    private boolean running = false;

    @Getter
    private Set<RepairWorkerKey> currentRepairWorkers = new HashSet<>();

    @Inject
    public SimpleRepairWorkerManager(
            Provider<QueueRepository> queueRepositoryProvider,
            RepairWorkerFactory repairWorkerFactory,
            RepairConfig config,
            @GenericScheduler ScheduledExecutorService scheduler) {
        this.queueRepositoryProvider = queueRepositoryProvider;
        this.repairWorkerFactory = repairWorkerFactory;
        this.config = config;
        this.scheduler = scheduler;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        logger.info("Starting repair manager");

        running = true;

        schedule();
    }

    @Override
    public synchronized void stop() {
        logger.info("Stopping repair manager");

        running = false;

        if (cancellationToken != null) {
            cancellationToken.cancel(false);

            cancellationToken = null;
        }

        stopActiveWorkers();
    }

    private void stopActiveWorkers() {
        if (!CollectionUtils.isEmpty(currentRepairWorkers)) {
            currentRepairWorkers.forEach(this::stopRepairWorker);

            currentRepairWorkers.clear();
        }
    }

    private void schedule() {
        cancellationToken =
                scheduler.scheduleWithFixedDelay(this::notifyChanges, 0, // start it immediately
                                                 config.getManagerRefreshRateSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public synchronized void notifyChanges() {
        try {
            if (!running) {
                return;
            }

            final Set<RepairWorkerKey> expectedWorkers = getExpectedWorkers();

            final ImmutableSet<RepairWorkerKey> newWorkers = Sets.difference(expectedWorkers, currentRepairWorkers).immutableCopy();

            final ImmutableSet<RepairWorkerKey> itemsToStop = Sets.difference(currentRepairWorkers, expectedWorkers).immutableCopy();

            if (newWorkers.size() == 0 && itemsToStop.size() == 0) {
                return;
            }

            logger.with("count", itemsToStop.size()).info("Removing workers");
            logger.with("count", newWorkers.size()).info("Workers to add");

            itemsToStop.forEach(this::stopRepairWorker);

            currentRepairWorkers.removeAll(itemsToStop);

            currentRepairWorkers.addAll(newWorkers);

            newWorkers.forEach(this::startRepairWorker);
        }
        catch (Exception ex) {
            logger.error(ex, "Error configuring active workers");
        }
    }

    private Set<RepairWorkerKey> getExpectedWorkers() {
        return queueRepositoryProvider.get()
                                      .getActiveQueues()
                                      .stream()
                                      .map(repairWorkerFactory::forQueue)
                                      .map(RepairWorkerKey::new)
                                      .collect(toSet());
    }

    private void startRepairWorker(final RepairWorkerKey repairWorkerKey) {
        repairWorkerKey.getRepairWorker().start();
    }

    private void stopRepairWorker(final RepairWorkerKey repairWorkerKey) {
        repairWorkerKey.getRepairWorker().stop();
    }
}
