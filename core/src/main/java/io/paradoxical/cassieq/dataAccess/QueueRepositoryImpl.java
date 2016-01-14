package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import io.paradoxical.cassieq.clustering.eventing.EventBus;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.PointerType;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueSizeCounterId;
import io.paradoxical.cassieq.model.QueueStatus;
import io.paradoxical.cassieq.model.events.QueueAddedEvent;
import io.paradoxical.cassieq.model.events.QueueDeletingEvent;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toList;

public class QueueRepositoryImpl extends RepositoryBase implements QueueRepository {

    private static final Logger logger = getLogger(QueueRepositoryImpl.class);

    private final Session session;

    private final EventBus eventBus;

    private final AccountName accountName;

    @Inject
    public QueueRepositoryImpl(
            @NonNull final Session session,
            final EventBus eventBus,
            @NonNull
            @Assisted AccountName accountName) {
        this.session = session;
        this.accountName = accountName;
        this.eventBus = eventBus;
    }


    /**
     * Move the queue into an inoperable state and create a job
     *
     * @param definition
     * @return
     */
    @Override
    public Optional<DeletionJob> tryMarkForDeletion(final QueueDefinition definition) {

        // insert a deletion job that copies config
        final boolean markedForDeletion = tryAdvanceQueueStatus(definition.getQueueName(), QueueStatus.PendingDelete);

        if (markedForDeletion) {
            final Optional<DeletionJob> deletionJob = insertDeletionJobIfNotExists(definition);

            if (tryAdvanceQueueStatus(definition.getQueueName(), QueueStatus.Deleting)) {
                // anyone can create a queue with the same name now

                logger.with("definition", definition).success("Marked for deletion");

                eventBus.publish(new QueueDeletingEvent());

                return deletionJob;
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Long> getQueueSize(final QueueDefinition definition) {
        final Statement where = QueryBuilder.select(Tables.QueueSize.SIZE)
                                            .from(Tables.QueueSize.TABLE_NAME)
                                            .where(eq(Tables.QueueSize.QUEUE_SIZE_COUNTER_ID, definition.getQueueSizeCounterId().get()));

        return Optional.ofNullable(getOne(session.execute(where), r -> r.getLong(Tables.QueueSize.SIZE)));
    }

    private Optional<DeletionJob> insertDeletionJobIfNotExists(final QueueDefinition definition) {
        final DeletionJob deletionJob = new DeletionJob(definition);

        final Statement insert = QueryBuilder.insertInto(Tables.DeletionJob.TABLE_NAME)
                                             .ifNotExists()
                                             .value(Tables.DeletionJob.QUEUE_NAME, deletionJob.getQueueName().get())
                                             .value(Tables.DeletionJob.ACCOUNT_NAME, accountName.get())
                                             .value(Tables.DeletionJob.VERSION, deletionJob.getVersion())
                                             .value(Tables.DeletionJob.BUCKET_SIZE, deletionJob.getBucketSize().get());

        if (session.execute(insert).wasApplied()) {
            return Optional.of(deletionJob);
        }

        return Optional.empty();
    }

    @Override
    public Optional<QueueDefinition> createQueue(@NonNull final QueueDefinition definition) {
        if (upsertQueueDefinition(definition)) {
            eventBus.publish(new QueueAddedEvent());

            return getActiveQueue(definition.getQueueName());
        }

        return Optional.empty();
    }

    /**
     * Set the status but only if its allowed to move to that status
     *
     * i.e. we MUST move gracefully between the states, no state jumping
     *
     * @param queueName
     * @param status
     */
    @Override
    public boolean tryAdvanceQueueStatus(
            @NonNull final QueueName queueName,
            @NonNull final QueueStatus status) {

        final Clause andIsPrevious = eq(Tables.Queue.STATUS, status.ordinal() - 1);
        final Clause orIsEqual = eq(Tables.Queue.STATUS, status.ordinal());

        Function<Clause, Boolean> applier = clause -> {
            final Statement update = QueryBuilder.update(Tables.Queue.TABLE_NAME)
                                                 .where(eq(Tables.Queue.QUEUE_NAME, queueName.get()))
                                                 .and(eq(Tables.Queue.ACCOUNT_NAME, accountName.get()))
                                                 .with(set(Tables.Queue.STATUS, status.ordinal()))
                                                 .onlyIf(clause);

            if (session.execute(update).wasApplied()) {
                logger.with("queue-name", queueName)
                      .with("new-status", status)
                      .success("Advancing queue status");

                return true;
            }

            return false;
        };

        if (applier.apply(andIsPrevious)) {
            return true;
        }

        return applier.apply(orIsEqual);
    }

    private boolean insertQueueIfNotExist(final QueueDefinition queueDefinition) {

        final int version = 0;

        final Insert insertQueue =
                QueryBuilder.insertInto(Tables.Queue.TABLE_NAME)
                            .ifNotExists()
                            .value(Tables.Queue.ACCOUNT_NAME, accountName.get())
                            .value(Tables.Queue.QUEUE_NAME, queueDefinition.getQueueName().get())
                            .value(Tables.Queue.VERSION, version)
                            .value(Tables.Queue.BUCKET_SIZE, queueDefinition.getBucketSize().get())
                            .value(Tables.Queue.DELETE_BUCKETS_AFTER_FINALIZATION, queueDefinition.getDeleteBucketsAfterFinalization())
                            .value(Tables.Queue.REPAIR_WORKER_POLL_FREQ_SECONDS, queueDefinition.getRepairWorkerPollFrequencySeconds())
                            .value(Tables.Queue.REPAIR_WORKER_TOMBSTONE_BUCKET_TIMEOUT_SECONDS, queueDefinition.getRepairWorkerTombstonedBucketTimeoutSeconds())
                            .value(Tables.Queue.QUEUE_SIZE_COUNTER_ID, getUniqueQueueCounterId(queueDefinition.getQueueName(), version).get())
                            .value(Tables.Queue.MAX_DELIVERY_COUNT, queueDefinition.getMaxDeliveryCount())
                            .value(Tables.Queue.STATUS, QueueStatus.Provisioning.ordinal());

        final boolean queueInserted = session.execute(insertQueue).wasApplied();

        if (queueInserted) {
            ensurePointers(queueDefinition);

            return tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Active);
        }

        return false;
    }

    private boolean upsertQueueDefinition(@NonNull final QueueDefinition queueDefinition) {
        final Logger upsertLogger = logger.with("queue-name", queueDefinition.getQueueName());

        queueDefinition.setAccountName(accountName);

        if (insertQueueIfNotExist(queueDefinition)) {
            upsertLogger.success("Created new queue");

            return true;
        }

        final Optional<QueueDefinition> currentQueueDefinitionOption = getQueueUnsafe(queueDefinition.getQueueName());

        if (!currentQueueDefinitionOption.isPresent()) {
            // a queue was deleted in the time that it was inactive + already existing
            // try creating the queue again since that meant that the deletion job
            // succeeded just after the insert statement attempted.

            upsertLogger.warn("The queue was deleted after being in an inactive state");

            return upsertQueueDefinition(queueDefinition);
        }

        final QueueDefinition currentQueueDefinition = currentQueueDefinitionOption.get();

        // not provisionable state
        if (currentQueueDefinition.getStatus().ordinal() < QueueStatus.Deleting.ordinal()) {
            upsertLogger.with("current-status", currentQueueDefinition.getStatus())
                        .with("version", currentQueueDefinition.getVersion())
                        .warn("Queue status does not allow provisioning");

            return false;
        }

        // make sure the pointers exist
        ensurePointers(currentQueueDefinition);

        // if however, an inactve queue exists, make sure only one person can
        // create the next active queue (prevent race conditions of multiple queues being created
        // of the same name that are active
        // first check the table name version table

        final int currentVersion = currentQueueDefinition.getVersion();
        final int nextVersion = currentVersion + 1;

        final QueueSizeCounterId newQueueCounterId = getUniqueQueueCounterId(currentQueueDefinition.getQueueName(), nextVersion);

        // update the tracking table to see who can grab the next version
        // only if the queue name status is inactive
        // if its available grab it
        final Statement updateQueueDefinitionStatement =
                QueryBuilder.update(Tables.Queue.TABLE_NAME)
                            .where(eq(Tables.Queue.QUEUE_NAME, queueDefinition.getQueueName().get()))
                            .and(eq(Tables.Queue.ACCOUNT_NAME, accountName.get()))
                            .with(set(Tables.Queue.VERSION, nextVersion))
                            .and(set(Tables.Queue.STATUS, QueueStatus.Active.ordinal()))
                            .and(set(Tables.Queue.BUCKET_SIZE, queueDefinition.getBucketSize().get()))
                            .and(set(Tables.Queue.MAX_DELIVERY_COUNT, queueDefinition.getMaxDeliveryCount()))
                            .and(set(Tables.Queue.QUEUE_SIZE_COUNTER_ID, newQueueCounterId.get()))
                            .onlyIf(eq(Tables.Queue.VERSION, currentVersion))
                            .and(gte(Tables.Queue.STATUS, QueueStatus.Deleting.ordinal()));

        final boolean queueUpdateApplied = session.execute(updateQueueDefinitionStatement).wasApplied();

        if (queueUpdateApplied) {
            upsertLogger.success("Update queue to active");
        }

        return queueUpdateApplied;
    }

    private QueueSizeCounterId getUniqueQueueCounterId(final QueueName queueName, final int version) {
        return QueueSizeCounterId.valueOf(String.format("%s_%s", UUID.randomUUID(), QueueId.valueOf(accountName, queueName, version)));
    }

    private void ensurePointers(final QueueDefinition queueDefinition) {
        insertQueueMonotonicValueIfNotExists(queueDefinition.getId());

        insertQueuePointerIfNotExists(queueDefinition.getId());
    }

    private void insertQueuePointerIfNotExists(@NonNull final QueueId queueId) {
        initializePointer(queueId, PointerType.BUCKET_POINTER, 0L);
        initializePointer(queueId, PointerType.INVISIBILITY_POINTER, -1L);
        initializePointer(queueId, PointerType.REPAIR_BUCKET, 0L);
    }

    private void insertQueueMonotonicValueIfNotExists(@NonNull final QueueId queueId) {
        final Statement insert = QueryBuilder.insertInto(Tables.Monoton.TABLE_NAME)
                                             .ifNotExists()
                                             .value(Tables.Monoton.VALUE, 0L)
                                             .value(Tables.Monoton.QUEUE_ID, queueId.get());

        session.execute(insert);
    }

    private void initializePointer(@NonNull final QueueId queueId, @NonNull final PointerType pointerType, @NonNull final Long value) {
        final Statement upsert = QueryBuilder.insertInto(Tables.Pointer.TABLE_NAME)
                                             .ifNotExists()
                                             .value(Tables.Pointer.VALUE, value)
                                             .value(Tables.Pointer.POINTER_TYPE, pointerType.toString())
                                             .value(Tables.Pointer.QUEUE_ID, queueId.get());

        session.execute(upsert);
    }

    /**
     * Returns the queue regardless of status
     *
     * @param queueName
     * @return
     */
    @Override
    public Optional<QueueDefinition> getQueueUnsafe(@NonNull final QueueName queueName) {
        final Select.Where queryOne =
                QueryBuilder.select().all()
                            .from(Tables.Queue.TABLE_NAME)
                            .where(eq(Tables.Queue.QUEUE_NAME, queueName.get()))
                            .and(eq(Tables.Queue.ACCOUNT_NAME, accountName.get()));

        final QueueDefinition result = getOne(session.execute(queryOne), QueueDefinition::fromRow);

        return Optional.ofNullable(result);
    }


    @Override
    public List<QueueDefinition> getQueues(QueueStatus status) {
        final Select query = QueryBuilder.select().all().from(Tables.Queue.TABLE_NAME);

        return session.execute(query)
                      .all()
                      .stream()
                      .map(QueueDefinition::fromRow)
                      .filter(queue -> queue.getStatus().equals(status))
                      .collect(toList());
    }

    @Override
    public Optional<QueueDefinition> getActiveQueue(final QueueName name) {
        final Optional<QueueDefinition> queue = getQueueUnsafe(name);

        return queue.filter(queueDef -> queueDef.getStatus() == QueueStatus.Active);
    }

    @Override
    public void deleteCompletionJob(final DeletionJob job) {
        final Statement delete = QueryBuilder.delete()
                                             .from(Tables.DeletionJob.TABLE_NAME)
                                             .where(eq(Tables.DeletionJob.QUEUE_NAME, job.getQueueName().get()))
                                             .and(eq(Tables.Queue.ACCOUNT_NAME, accountName.get()))
                                             .and(eq(Tables.DeletionJob.VERSION, job.getVersion()));

        if (session.execute(delete).wasApplied()) {
            logger.with(job).success("Removed deletion job");

            if (deleteIfInActive(job.getQueueName())) {
                logger.with("queue-name", job.getQueueName())
                      .with("version", job.getVersion())
                      .info("Deleted queue definition since it was inactive");
            }
        }
    }

    @Override
    public void deleteQueueStats(QueueSizeCounterId counterId) {
        final Statement delete = QueryBuilder.delete()
                                             .from(Tables.QueueSize.TABLE_NAME)
                                             .where(eq(Tables.QueueSize.QUEUE_SIZE_COUNTER_ID, counterId.get()));

        if (session.execute(delete).wasApplied()) {
            logger.with("counter_id", counterId)
                  .success("Deleted queue stats");
        }
    }


    private boolean deleteIfInActive(QueueName queueName) {
        final Statement delete = QueryBuilder.delete()
                                             .from(Tables.Queue.TABLE_NAME)
                                             .onlyIf(eq(Tables.Queue.STATUS, QueueStatus.Inactive.ordinal()))
                                             .where(eq(Tables.Queue.ACCOUNT_NAME, accountName.get()))
                                             .and(eq(Tables.Queue.QUEUE_NAME, queueName.get()));

        return session.execute(delete).wasApplied();
    }
}
