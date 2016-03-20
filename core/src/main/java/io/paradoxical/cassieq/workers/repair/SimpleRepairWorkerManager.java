package io.paradoxical.cassieq.workers.repair;

import com.godaddy.logging.Logger;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.paradoxical.cassieq.clustering.allocation.ResourceAllocator;
import io.paradoxical.cassieq.clustering.allocation.ResourceConfig;
import io.paradoxical.cassieq.clustering.allocation.ResourceGroup;
import io.paradoxical.cassieq.clustering.allocation.ResourceIdentity;
import io.paradoxical.cassieq.configurations.RepairConfig;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.modules.annotations.GenericScheduler;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toSet;

public class SimpleRepairWorkerManager implements RepairWorkerManager {
    private static final Logger logger = getLogger(SimpleRepairWorkerManager.class);

    private final RepairWorkerFactory repairWorkerFactory;
    private final RepairConfig config;
    private final ScheduledExecutorService scheduler;
    private final DataContextFactory dataContextFactory;
    private ScheduledFuture<?> cancellationToken;
    final ResourceAllocator allocator;

    private final Object lock = new Object();

    private boolean running = false;

    private Set<RepairWorkerKey> expectedRepairWorkers = new HashSet<>();

    @Inject
    public SimpleRepairWorkerManager(
            DataContextFactory dataContextFactory,
            RepairWorkerFactory repairWorkerFactory,
            ResourceAllocator.Factory resourceFactory,
            RepairConfig config,
            @GenericScheduler ScheduledExecutorService scheduler) {
        this.dataContextFactory = dataContextFactory;
        this.repairWorkerFactory = repairWorkerFactory;
        this.config = config;
        this.scheduler = scheduler;

        this.allocator = resourceFactory.getAllocator(getResourceConfig(),
                                                      this::getAllocateableWorkers,
                                                      this::setClaimedRepairWorkers);
    }

    private ResourceConfig getResourceConfig() {
        return new ResourceConfig(ResourceGroup.valueOf("repair-workers"), 100);
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        logger.info("Starting repair manager");

        running = true;

        cancellationToken = scheduler.scheduleWithFixedDelay(this::claim, 0, config.getManagerRefreshRateSeconds(), TimeUnit.SECONDS);
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

        try {
            allocator.close();
        }
        catch (Exception e) {
            logger.error(e, "Error closing allocator");
        }
    }

    private void stopActiveWorkers() {
        if (!CollectionUtils.isEmpty(expectedRepairWorkers)) {
            expectedRepairWorkers.forEach(this::stopRepairWorker);

            expectedRepairWorkers.clear();
        }
    }

    public synchronized void claim() {
        logger.info("Claiming resources");

        allocator.claim();
    }

    @Override
    public synchronized void notifyChanges() {
        if (!running) {
            return;
        }

        claim();

        applyChanges();
    }

    private void applyChanges() {
        try {
            if (!running) {
                return;
            }

            final Set<RepairWorkerKey> expectedWorkers = getClaimedRepairWorkers();

            final ImmutableSet<RepairWorkerKey> newWorkers = Sets.difference(expectedWorkers, expectedRepairWorkers).immutableCopy();

            final ImmutableSet<RepairWorkerKey> itemsToStop = Sets.difference(expectedRepairWorkers, expectedWorkers).immutableCopy();

            if (newWorkers.size() == 0 && itemsToStop.size() == 0) {
                return;
            }

            logger.with("count", itemsToStop.size()).info("Removing workers");
            logger.with("count", newWorkers.size()).info("Workers to add");

            itemsToStop.forEach(this::stopRepairWorker);

            expectedRepairWorkers.removeAll(itemsToStop);

            expectedRepairWorkers.addAll(newWorkers);

            newWorkers.forEach(this::startRepairWorker);
        }
        catch (Exception ex) {
            logger.error(ex, "Error configuring active workers");
        }
    }


    private Set<RepairWorkerKey> getAllRepairKeys() {
        final List<AccountDefinition> allAccounts = dataContextFactory.getAccountRepository().getAllAccounts();

        return allAccounts.stream().flatMap(account ->
                                                    dataContextFactory.forAccount(account.getAccountName())
                                                                      .getActiveQueues()
                                                                      .stream()
                                                                      .map(repairWorkerFactory::forQueue)
                                                                      .map(RepairWorkerKey::new))
                          .collect(toSet());
    }

    private Set<ResourceIdentity> getAllocateableWorkers() {
        return getAllRepairKeys().stream()
                                 .map(key -> ResourceIdentity.valueOf(key.getQueueDefinition().getId()))
                                 .collect(Collectors.toSet());
    }

    private void startRepairWorker(final RepairWorkerKey repairWorkerKey) {
        repairWorkerKey.getRepairWorker().start();
    }

    private void stopRepairWorker(final RepairWorkerKey repairWorkerKey) {
        repairWorkerKey.getRepairWorker().stop();
    }

    public void waitForChanges() throws InterruptedException {
        synchronized (lock) {
            lock.wait();
        }
    }

    public synchronized Set<RepairWorkerKey> getClaimedRepairWorkers() {
        return expectedRepairWorkers;
    }

    private synchronized void setClaimedRepairWorkers(final Set<ResourceIdentity> resourceIdentities) {
        final Set<QueueId> expectedQueueIds = resourceIdentities.stream().map(identity -> QueueId.valueOf(identity.get())).collect(toSet());

        final Set<RepairWorkerKey> expectedWorkers =
                getAllRepairKeys().stream()
                                  .filter(i -> expectedQueueIds.contains(i.getQueueDefinition().getId()))
                                  .collect(toSet());

        this.expectedRepairWorkers = expectedWorkers;

        applyChanges();

        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
