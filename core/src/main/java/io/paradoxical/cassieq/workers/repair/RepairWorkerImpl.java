package io.paradoxical.cassieq.workers.repair;

import com.codahale.metrics.MetricRegistry;
import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.QueueDataContext;
import io.paradoxical.cassieq.model.BucketPointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import io.paradoxical.cassieq.model.time.Clock;
import io.paradoxical.cassieq.modules.annotations.RepairPool;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Seconds;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static com.godaddy.logging.LoggerFactory.getLogger;

public class RepairWorkerImpl implements RepairWorker {
    private final Clock clock;
    private final ScheduledExecutorService scheduledExecutorService;
    private final MetricRegistry metricRegistry;
    private final QueueDefinition queueDefinition;

    private Logger logger = getLogger(RepairWorkerImpl.class);

    private final QueueDataContext dataContext;

    private volatile boolean isStarted;

    private final Object nextRun = new Object();

    private ScheduledFuture<?> activeSchedule;

    private final Runnable republishStatCounter;
    private final Runnable danglingDetectedCounter;
    private final Runnable deletingFinalizedCounter;

    @Inject
    public RepairWorkerImpl(
            DataContextFactory factory,
            Clock clock,
            @RepairPool ScheduledExecutorService executorService,
            MetricRegistry metricRegistry,
            @Assisted QueueDefinition definition) {
        this.clock = clock;
        scheduledExecutorService = executorService;
        this.metricRegistry = metricRegistry;
        queueDefinition = definition;
        dataContext = factory.forQueue(definition);

        final String repairMetricKey = name("repair", queueDefinition.getQueueName().get());

        republishStatCounter = () -> metricRegistry.counter(name(repairMetricKey, "republish")).inc();
        danglingDetectedCounter = () -> metricRegistry.counter(name(repairMetricKey, "dangling-detected")).inc();
        deletingFinalizedCounter = () -> metricRegistry.counter(name(repairMetricKey, "deleting-finalized")).inc();

        logger = logger.with("queue-name", definition.getQueueName())
                       .with("verison", definition.getVersion());
    }

    @Override
    public void start() {
        isStarted = true;

        logger.success("Starting repairer");

        schedule();
    }

    @Override
    public void stop() {
        isStarted = false;

        if (activeSchedule != null) {
            activeSchedule.cancel(false);
        }
    }

    @Override
    public QueueDefinition forDefinition() {
        return queueDefinition;
    }

    public void waitForNextRun() throws InterruptedException {
        synchronized (nextRun) {
            nextRun.wait();
        }
    }

    private void schedule() {
        activeSchedule = scheduledExecutorService.scheduleWithFixedDelay(this::process,
                                                                         clock.jitter(20), // randomly start within a 20 second range as a jitter
                                                                         queueDefinition.getRepairWorkerPollFrequencySeconds(),
                                                                         TimeUnit.SECONDS);
    }

    private void process() {
        try {
            final Optional<RepairContext> firstBucketToMonitor = findFirstBucketToMonitor();

            if (firstBucketToMonitor.isPresent()) {
                watchBucket(firstBucketToMonitor.get());

                synchronized (nextRun) {
                    nextRun.notifyAll();
                }
            }
        }
        catch (Throwable ex) {
            logger.error(ex, "Error processing!");
        }
    }

    private RepairBucketPointer getCurrentBucket() {
        return dataContext.getPointerRepository().getRepairCurrentBucketPointer();
    }

    private void watchBucket(RepairContext context) {
        waitForTimeout(context.getTombstonedAt());

        if (!isStarted) {
            return;
        }

        List<Message> messages = dataContext.getMessageRepository().getMessages(context.getPointer());

        messages.stream().filter(message -> !message.isAcked() && message.isVisible(clock) && message.getDeliveryCount() == 0)
                .forEach(this::republishMessage);

        advance(context.getPointer());

        // check if all is acked now before we give up
        messages = dataContext.getMessageRepository().getMessages(context.getPointer());

        // only delete them all if they are all already acked
        if (messages.stream().allMatch(Message::isAcked)) {
            deleteMessagesInBucket(context.getPointer());
        }
        else {
            danglingDetectedCounter.run();
        }

        markAsFinalized(context.getPointer());
    }

    private void waitForTimeout(final DateTime tombstoneTime) {

        final DateTime plus = tombstoneTime.plus(Duration.ofSeconds(queueDefinition.getRepairWorkerTombstonedBucketTimeoutSeconds()).toMillis());

        final Instant now = clock.now();

        final Seconds seconds = Seconds.secondsBetween(now, plus);

        logger.with("tombstone-time", tombstoneTime)
              .with("now", now)
              .with("seconds-to-wait", seconds)
              .debug("Need to wait for bucket to be time closed");

        if (seconds.isGreaterThan(Seconds.ZERO)) {
            // wait for the repair worker timeout
            try {
                clock.sleepFor(seconds.toStandardDuration());
            }
            catch (InterruptedException e) {
                // ok
            }
        }

        logger.success("Bucket should be closed");
    }

    /**
     * Scan through the message table from the last known pointer
     * and find the first bucket that exists and isn't tombstoned
     *
     * @return
     */
    private Optional<RepairContext> findFirstBucketToMonitor() {
        RepairBucketPointer currentBucket = getCurrentBucket();

        while (isStarted) {
            // first bucket that is tombstoned and is unfilled
            final MessageRepository messageRepository = dataContext.getMessageRepository();

            final Optional<DateTime> tombstoneTime = messageRepository.tombstoneExists(currentBucket);

            if (tombstoneTime.isPresent()) {
                final List<Message> messages = messageRepository.getMessages(currentBucket);

                if (messages.size() == queueDefinition.getBucketSize().get() && messages.stream().allMatch(Message::isAcked)) {
                    deleteMessagesInBucket(currentBucket);

                    currentBucket = advance(currentBucket);

                    logger.with(currentBucket).info("Found full bucket, advancing");

                    // look for next bucket
                    continue;
                }

                else {
                    logger.with(currentBucket).info("Found tombstoned bucket, going to watch");

                    // found a bucket that is tombestoned, and need to now wait for the timeout
                    // before processing all messages and moving on
                    return Optional.of(new RepairContext(currentBucket, tombstoneTime.get()));
                }
            }

            // on an active bucket that isn't tombstoned, just come back later and wait for tombstone
            return Optional.empty();
        }

        return Optional.empty();
    }

    private void deleteMessagesInBucket(final RepairBucketPointer currentBucket) {
        if (queueDefinition.getDeleteBucketsAfterFinaliziation()) {
            dataContext.getMessageRepository().deleteAllMessages(currentBucket);

            deletingFinalizedCounter.run();
        }

        markAsFinalized(currentBucket);
    }

    private void markAsFinalized(final RepairBucketPointer currentBucket) {
        if (dataContext.getMessageRepository().finalize(currentBucket)) {
            logger.with("current-repair-bucket", currentBucket).info("Finalizing bucket");
        }
    }

    private void republishMessage(Message message) {
        try {
            final Logger withLogger = logger.with(message);

            republishStatCounter.run();

            final MonotonicIndex nextIndex = dataContext.getMonotonicRepository().nextMonotonic();

            withLogger.debug("Republishing message");

            dataContext.getMessageRepository().putMessage(message.createNewWithIndex(nextIndex));

            withLogger.success("Republished message");

            if (dataContext.getMessageRepository().ackMessage(message)) {
                withLogger
                        .with("next-index", nextIndex)
                        .success("Message neededrepublishing, acked original and publishing new one");
            }
        }
        catch (Exception e) {
            logger.error(e, "Error publishing message");

            throw new RuntimeException(e);
        }
    }

    private RepairBucketPointer advance(final RepairBucketPointer currentBucket) {
        logger.info("Advancing bucket");

        BucketPointer monotonBucket = getCurrentMonotonBucket();

        // dont let the repair bucket pointer advance past the bucket of the current monoton
        if (currentBucket.next().get() > monotonBucket.get()) {
            logger.with("attempted-next-bucket", currentBucket.next())
                  .with("current-monton-bucket", monotonBucket)
                  .with("bucket-size", queueDefinition.getBucketSize())
                  .warn("Attempted to move past monoton bucket, but limited");

            return RepairBucketPointer.valueOf(monotonBucket.get());
        }

        final RepairBucketPointer repairBucketPointer = dataContext.getPointerRepository().advanceRepairBucketPointer(currentBucket, currentBucket.next());

        logger.with("now-repair-pointer", repairBucketPointer).info("New bucket");

        return repairBucketPointer;
    }

    private BucketPointer getCurrentMonotonBucket() {
        final MonotonicIndex currentMonton = dataContext.getMonotonicRepository().getCurrent();

        return RepairBucketPointer.valueOf(currentMonton.toBucketPointer(queueDefinition.getBucketSize()).get());
    }
}
