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
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gt;
import static com.datastax.driver.core.querybuilder.QueryBuilder.lt;
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
    public boolean tryMarkForDeletion(final QueueDefinition definition) {
        final boolean markedForDeletion = tryAdvanceQueueStatus(definition.getQueueName(), QueueStatus.Deleting);

        if (markedForDeletion) {
            logger.with("definition", definition).success("Marked for deletion");
        }

        return markedForDeletion;
    }

    @Override
    public boolean tryMarkQueueInactive(final QueueDefinition definition) {
        final Update.Conditions updateTrackingTable =
                QueryBuilder.update(Tables.Queue.TABLE_NAME)
                            .where(eq(Tables.Queue.QUEUE_NAME, definition.getQueueName().get()))
                            .with(set(Tables.Queue.STATUS, QueueStatus.Inactive.ordinal()))
                            .onlyIf(eq(Tables.Queue.VERSION, definition.getVersion()))
                            .and(eq(Tables.Queue.STATUS, QueueStatus.Deleting.ordinal()));

        return session.execute(updateTrackingTable).wasApplied();
    }


    @Override
    public boolean createQueue(@NonNull final QueueDefinition definition) {
        return upsertQueueDefinition(definition);
    }

    private boolean setQueueDefinitionStatus(
            @NonNull final QueueName queueName,
            @NonNull final QueueStatus status) {

        final Statement update = QueryBuilder.update(Tables.Queue.TABLE_NAME)
                                             .where(eq(Tables.Queue.QUEUE_NAME, queueName.get()))
                                             .with(set(Tables.Queue.STATUS, status.ordinal()));

        session.execute(update);

        return true;
    }

    /**
     * Set the status but only if its allowed to move to that status
     *
     * @param queueName
     * @param status
     */
    public boolean tryAdvanceQueueStatus(
            @NonNull final QueueName queueName,
            @NonNull final QueueStatus status) {

        final Statement update = QueryBuilder.update(Tables.Queue.TABLE_NAME)
                                             .where(eq(Tables.Queue.QUEUE_NAME, queueName.get()))
                                             .with(set(Tables.Queue.STATUS, status.ordinal()))
                                             .onlyIf(lt(Tables.Queue.STATUS, status.ordinal()));

        return session.execute(update).wasApplied();
    }

    private boolean insertQueueIfNotExist(final QueueDefinition queueDefinition) {

        final Insert insertQueue = QueryBuilder.insertInto(Tables.Queue.TABLE_NAME)
                                               .ifNotExists()
                                               .value(Tables.Queue.QUEUE_NAME, queueDefinition.getQueueName().get())
                                               .value(Tables.Queue.VERSION, 0)
                                               .value(Tables.Queue.BUCKET_SIZE, queueDefinition.getBucketSize().get())
                                               .value(Tables.Queue.MAX_DELIVERY_COUNT, queueDefinition.getMaxDeliveryCount())
                                               .value(Tables.Queue.STATUS, QueueStatus.Inactive.ordinal());

        final boolean queueInserted = session.execute(insertQueue).wasApplied();

        if (queueInserted) {

            resetQueueMonotonicValue(queueDefinition.getQueueName());

            resetQueuePointers(queueDefinition.getQueueName());

            return setQueueDefinitionStatus(queueDefinition.getQueueName(), QueueStatus.Active);
        }

        return false;
    }

    private boolean upsertQueueDefinition(@NonNull final QueueDefinition queueDefinition) {

        if (insertQueueIfNotExist(queueDefinition)) {
            // easy/happy path. nothing was there before
            return true;
        }


        final Optional<QueueDefinition> currentQueueDefinitionOption = getQueue(queueDefinition.getQueueName());

        if (!currentQueueDefinitionOption.isPresent()) {
            throw new RuntimeException("this should be impossible because we just inserted the queue");
        }

        final QueueDefinition currentQueueDefinition = currentQueueDefinitionOption.get();

        if (currentQueueDefinition.getStatus() != QueueStatus.Inactive) {
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
                            .and(eq(Tables.Queue.STATUS, QueueStatus.Inactive.ordinal()));

        final boolean queueUpdateApplied = session.execute(insertTrackingId).wasApplied();

        logger.with(queueDefinition).success("Created queue");

        return queueUpdateApplied;
    }

    private void resetQueuePointers(@NonNull final QueueName queueName) {

        initializePointer(queueName, PointerType.BUCKET_POINTER, 0L);
        initializePointer(queueName, PointerType.INVISIBILITY_POINTER, -1L);
        initializePointer(queueName, PointerType.REPAIR_BUCKET, 0L);
    }

    private void resetQueueMonotonicValue(@NonNull final QueueName queueName) {
        final Update.Where upsert = QueryBuilder.update(Tables.Monoton.TABLE_NAME)
                                                .with(set(Tables.Monoton.VALUE, 0L))
                                                .where(eq(Tables.Monoton.QUEUE_NAME, queueName.get()));

        session.execute(upsert);
    }

    private void initializePointer(@NonNull final QueueName queueName, @NonNull final PointerType pointerType, @NonNull final Long value) {
        final Update.Where upsert = QueryBuilder.update(Tables.Pointer.TABLE_NAME)
                                                .with(set(Tables.Pointer.VALUE, value))
                                                .and(set(Tables.Pointer.POINTER_TYPE, pointerType.toString()))
                                                .where(eq(Tables.Pointer.QUEUE_NAME, queueName.get()));

        session.execute(upsert);
    }

    @Override
    public Optional<QueueDefinition> getQueue(@NonNull final QueueName queueName) {
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
        final Optional<QueueDefinition> queue = getQueue(name);

        return queue.filter(queueDef -> queueDef.getStatus() == QueueStatus.Active);
    }
}
