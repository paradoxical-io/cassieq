package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.PointerType;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.lte;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toList;

public class QueueRepositoryImpl extends RepositoryBase implements QueueRepository {

    private static final Logger logger = getLogger(QueueRepositoryImpl.class);

    private final Session session;

    @Inject
    public QueueRepositoryImpl(@NonNull final Session session) {
        this.session = session;
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

                return deletionJob;
            }
        }

        return Optional.empty();
    }

    private Optional<DeletionJob> insertDeletionJobIfNotExists(final QueueDefinition definition) {
        final DeletionJob deletionJob = new DeletionJob(definition);

        final Statement insert = QueryBuilder.insertInto(Tables.DeletionJob.TABLE_NAME)
                                             .ifNotExists()
                                             .value(Tables.DeletionJob.QUEUE_NAME, deletionJob.getQueueName().get())
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
            return getActiveQueue(definition.getQueueName());
        }

        return Optional.empty();
    }

    /**
     * Set the status but only if its allowed to move to that status
     *
     * @param queueName
     * @param status
     */
    @Override
    public boolean tryAdvanceQueueStatus(
            @NonNull final QueueName queueName,
            @NonNull final QueueStatus status) {

        final Statement update = QueryBuilder.update(Tables.Queue.TABLE_NAME)
                                             .where(eq(Tables.Queue.QUEUE_NAME, queueName.get()))
                                             .with(set(Tables.Queue.STATUS, status.ordinal()))
                                             .onlyIf(lte(Tables.Queue.STATUS, status.ordinal()));

        if (session.execute(update).wasApplied()) {
            logger.with("queue-name", queueName)
                  .with("new-status", status)
                  .success("Advancing queue status");

            return true;
        }

        return false;
    }

    @Override
    public boolean deleteIfInActive(QueueName queueName) {
        final Statement delete = QueryBuilder.delete()
                                             .from(Tables.Queue.TABLE_NAME)
                                             .onlyIf(eq(Tables.Queue.STATUS, QueueStatus.Inactive.ordinal()))
                                             .where(eq(Tables.Queue.QUEUE_NAME, queueName.get()));

        return session.execute(delete).wasApplied();
    }

    private boolean insertQueueIfNotExist(final QueueDefinition queueDefinition) {

        final Insert insertQueue =
                QueryBuilder.insertInto(Tables.Queue.TABLE_NAME)
                            .ifNotExists()
                            .value(Tables.Queue.QUEUE_NAME, queueDefinition.getQueueName().get())
                            .value(Tables.Queue.VERSION, 0)
                            .value(Tables.Queue.BUCKET_SIZE, queueDefinition.getBucketSize().get())
                            .value(Tables.Queue.DELETE_BUCKETS_AFTER_FINALIZATION, queueDefinition.getDeleteBucketsAfterFinaliziation())
                            .value(Tables.Queue.REPAIR_WORKER_POLL_FREQ_SECONDS, queueDefinition.getRepairWorkerPollFrequencySeconds())
                            .value(Tables.Queue.REPAIR_WORKER_TOMBSTONE_BUCKET_TIMEOUT_SECONDS, queueDefinition.getRepairWorkerTombstonedBucketTimeoutSeconds())
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

        // update the tracking table to see who can grab the next version
        // only if the queue name status is inactive
        // if its available grab it
        final Statement insertTrackingId =
                QueryBuilder.update(Tables.Queue.TABLE_NAME)
                            .where(eq(Tables.Queue.QUEUE_NAME, queueDefinition.getQueueName().get()))
                            .with(set(Tables.Queue.VERSION, nextVersion))
                            .and(set(Tables.Queue.STATUS, QueueStatus.Active.ordinal()))
                            .and(set(Tables.Queue.BUCKET_SIZE, queueDefinition.getBucketSize().get()))
                            .and(set(Tables.Queue.MAX_DELIVERY_COUNT, queueDefinition.getMaxDeliveryCount()))
                            .onlyIf(eq(Tables.Queue.VERSION, currentVersion))
                            .and(gte(Tables.Queue.STATUS, QueueStatus.Deleting.ordinal()));

        final boolean queueUpdateApplied = session.execute(insertTrackingId).wasApplied();

        if (queueUpdateApplied) {
            upsertLogger.success("Update queue to active");
        }

        return queueUpdateApplied;
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
                            .where(eq(Tables.Queue.QUEUE_NAME, queueName.get()));

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
                                             .and(eq(Tables.DeletionJob.VERSION, job.getVersion()));

        session.execute(delete);

        if (deleteIfInActive(job.getQueueName())) {
            logger.with("queue-name", job.getQueueName())
                  .with("version", job.getVersion())
                  .info("Deleted queue definition since it was inactive");
        }
    }
}
