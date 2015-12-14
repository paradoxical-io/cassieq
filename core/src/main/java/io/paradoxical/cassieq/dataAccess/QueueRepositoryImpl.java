package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
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
        final DeletionJob deletionJob = new DeletionJob(definition.getQueueName(),
                                                        definition.getVersion(),
                                                        definition.getBucketSize());


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
    public boolean createQueue(@NonNull final QueueDefinition definition) {
        return upsertQueueDefinition(definition);
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

        return session.execute(update).wasApplied();
    }

    @Override
    public boolean deleteIfInActive(QueueName queueName) {
        final Statement delete = QueryBuilder.delete()
                                             .from(Tables.Queue.TABLE_NAME)
                                             .ifExists()
                                             .onlyIf(eq(Tables.Queue.STATUS, QueueStatus.Inactive.ordinal()))
                                             .where(eq(Tables.Queue.QUEUE_NAME, queueName.get()));

        return session.execute(delete).wasApplied();
    }

    private boolean insertQueueIfNotExist(final QueueDefinition queueDefinition) {

        final Insert insertQueue = QueryBuilder.insertInto(Tables.Queue.TABLE_NAME)
                                               .ifNotExists()
                                               .value(Tables.Queue.QUEUE_NAME, queueDefinition.getQueueName().get())
                                               .value(Tables.Queue.VERSION, 0)
                                               .value(Tables.Queue.BUCKET_SIZE, queueDefinition.getBucketSize().get())
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
        if (insertQueueIfNotExist(queueDefinition)) {
            // easy/happy path. nothing was there before
            return true;
        }

        final Optional<QueueDefinition> currentQueueDefinitionOption = getQueueUnsafe(queueDefinition.getQueueName());

        if (!currentQueueDefinitionOption.isPresent()) {
            throw new RuntimeException("this should be impossible because we just inserted the queue");
        }

        final QueueDefinition currentQueueDefinition = currentQueueDefinitionOption.get();

        // always insert if not exist pointers regardless of state
        ensurePointers(currentQueueDefinition);

        // not provisionable state
        if (currentQueueDefinition.getStatus().ordinal() < QueueStatus.Deleting.ordinal()) {
            return false;
        }

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
            logger.with(queueDefinition).success("Created queue");
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
        final Update.Where upsert = QueryBuilder.update(Tables.Monoton.TABLE_NAME)
                                                .with(set(Tables.Monoton.VALUE, 0L))
                                                .where(eq(Tables.Monoton.QUEUE_ID, queueId.get()));

        session.execute(upsert);
    }

    private void initializePointer(@NonNull final QueueId queueId, @NonNull final PointerType pointerType, @NonNull final Long value) {
        final Update.Where upsert = QueryBuilder.update(Tables.Pointer.TABLE_NAME)
                                                .with(set(Tables.Pointer.VALUE, value))
                                                .and(set(Tables.Pointer.POINTER_TYPE, pointerType.toString()))
                                                .where(eq(Tables.Pointer.QUEUE_ID, queueId.get()));

        session.execute(upsert);
    }

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
    public List<QueueDefinition> getActiveQueues() {
        final Select query = QueryBuilder.select().all().from(Tables.Queue.TABLE_NAME);

        return session.execute(query)
                      .all()
                      .stream()
                      .map(QueueDefinition::fromRow)
                      .filter(queue -> queue.getStatus().equals(QueueStatus.Active))
                      .collect(toList());
    }

    @Override
    public Optional<QueueDefinition> getActiveQueue(final QueueName name) {
        final Optional<QueueDefinition> queue = getQueueUnsafe(name);

        return queue.filter(queueDef -> queueDef.getStatus() == QueueStatus.Active);
    }

    @Override
    public void deleteCompletionJob(final DeletionJob queue) {
        final Statement delete = QueryBuilder.delete()
                                             .from(Tables.DeletionJob.TABLE_NAME)
                                             .where(eq(Tables.DeletionJob.QUEUE_NAME, queue.getQueueName().get()))
                                             .and(eq(Tables.DeletionJob.VERSION, queue.getVersion()));

        session.execute(delete);
    }
}
