package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueExistsError;
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
    public void markForDeletion(final QueueDefinition definition) {
        if(trySetQueueDefinitionStatus(definition.getId(), QueueStatus.Deleting)) {
            if (trySetQueueNameAvailableStatus(definition, QueueStatus.Inactive)) {
                logger.with("definition", definition).success("Marked for deletion");
            }
        }
    }

    private boolean trySetQueueNameAvailableStatus(final QueueDefinition definition, final QueueStatus status) {
        final Statement update =
                QueryBuilder.update(Tables.QueueNameManager.TABLE_NAME)
                            .where(eq(Tables.QueueNameManager.QUEUE_NAME, definition.getQueueName().get()))
                            .with(set(Tables.QueueNameManager.STATUS, status.ordinal()))
                            .onlyIf(eq(Tables.QueueNameManager.VERSION, definition.getVersion()));

        return session.execute(update).wasApplied();
    }

    @Override
    public void createQueue(@NonNull final QueueDefinition definition) throws QueueExistsError {
        insertQueueRecord(definition);

        initializeMonotonicValue(definition.getId());

        initializePointers(definition.getId());
    }

    /**
     * Set the status but only if its allowed to move to that status
     * @param queueId
     * @param status
     */
    @Override
    public boolean trySetQueueDefinitionStatus(@NonNull final QueueId queueId, @NonNull final QueueStatus status) {
        final Statement update = QueryBuilder.update(Tables.Queue.TABLE_NAME)
                                             .where(eq(Tables.Queue.QUEUE_ID, queueId.get()))
                                             .with(set(Tables.Queue.STATUS, status.ordinal()))
                                             .onlyIf(lt(Tables.Queue.STATUS, status.ordinal()));

        return session.execute(update).wasApplied();
    }

    private void insertQueueRecord(@NonNull final QueueDefinition queueDefinition) throws QueueExistsError {
        // if an active queue exists you can't create another one of the same name
        if (getActiveQueue(queueDefinition.getQueueName()).isPresent()) {
            throw new QueueExistsError(queueDefinition);
        }

        // if however, an inactve queue exists, make sure only one person can
        // create the next active queue (prevent race conditions of multiple queues being created
        // of the same name that are active
        // first check the table name version table
        final Select.Where activeQueueIdSelect = QueryBuilder.select()
                                                             .column(Tables.QueueNameManager.VERSION)
                                                             .from(Tables.QueueNameManager.TABLE_NAME)
                                                             .where(eq(Tables.QueueNameManager.QUEUE_NAME, queueDefinition.getQueueName().get()));

        final Row row = session.execute(activeQueueIdSelect).one();

        int currentVersion;

        if (row == null) {
            currentVersion = seedQueueNameIdGeneratorTable(queueDefinition);
        }
        else {
            currentVersion = row.getInt(0);
        }

        final int nextVersion = currentVersion + 1;

        // update the tracking table to see who can grab the next version
        // only if the queue name status is inactive
        // if its available grab it
        final Statement insertTrackingId = QueryBuilder.update(Tables.QueueNameManager.TABLE_NAME)
                                                       .where(eq(Tables.QueueNameManager.QUEUE_NAME, queueDefinition.getQueueName().get()))
                                                       .with(set(Tables.QueueNameManager.VERSION, nextVersion))
                                                       .and(set(Tables.Queue.STATUS, QueueStatus.Active.ordinal()))
                                                       .onlyIf(eq(Tables.QueueNameManager.VERSION, currentVersion))
                                                       .and(eq(Tables.QueueNameManager.STATUS, QueueStatus.Inactive.ordinal()));

        if (!session.execute(insertTrackingId).wasApplied()) {
            throw new QueueExistsError(queueDefinition);
        }

        final QueueId queueId = QueueId.valueOf(queueDefinition.getQueueName(), nextVersion);

        // if we're still the one who grabbed the version then insert into the table
        final Insert insertQueue = QueryBuilder.insertInto(Tables.Queue.TABLE_NAME)
                                               .ifNotExists()
                                               .value(Tables.Queue.QUEUE_ID, queueId.get())
                                               .value(Tables.Queue.VERSION, nextVersion)
                                               .value(Tables.Queue.BUCKET_SIZE, queueDefinition.getBucketSize().get())
                                               .value(Tables.Queue.QUEUE_NAME, queueDefinition.getQueueName().get())
                                               .value(Tables.Queue.MAX_DELIVERY_COUNT, queueDefinition.getMaxDeliveryCount())
                                               .value(Tables.Queue.STATUS, QueueStatus.Active.ordinal());

        if (!session.execute(insertQueue).wasApplied()) {
            throw new QueueExistsError(queueDefinition);
        }

        queueDefinition.setId(queueId);

        queueDefinition.setVersion(nextVersion);

        logger.with(queueDefinition).success("Created queue");
    }

    /**
     * Inserts the queue definition into the tracking table if no queue with this name was ever made
     *
     * @param queueDefinition
     */
    private int seedQueueNameIdGeneratorTable(@NonNull final QueueDefinition queueDefinition) throws QueueExistsError {

        final int initialVersion = 0;

        final Statement value = QueryBuilder.insertInto(Tables.QueueNameManager.TABLE_NAME)
                                            .ifNotExists()
                                            .value(Tables.QueueNameManager.QUEUE_NAME, queueDefinition.getQueueName().get())
                                            .value(Tables.QueueNameManager.STATUS, QueueStatus.Inactive.ordinal())
                                            .value(Tables.QueueNameManager.VERSION, initialVersion);


        if (!session.execute(value).wasApplied()) {
            throw new QueueExistsError(queueDefinition);
        }

        logger.with("queue-name", queueDefinition.getQueueName())
              .debug("Seeding queue tracking table for new queue name");

        return initialVersion;
    }

    private void initializePointers(@NonNull final QueueId queueId) {

        initializePointer(queueId, PointerType.BUCKET_POINTER, 0L);
        initializePointer(queueId, PointerType.INVISIBILITY_POINTER, -1L);
        initializePointer(queueId, PointerType.REPAIR_BUCKET, 0L);
    }

    private void initializeMonotonicValue(@NonNull final QueueId queueId) {
        Statement statement = QueryBuilder.insertInto(Tables.Monoton.TABLE_NAME)
                                          .value(Tables.Monoton.QUEUE_ID, queueId.get())
                                          .value(Tables.Monoton.VALUE, 0L)
                                          .ifNotExists();

        session.execute(statement);
    }

    private void initializePointer(@NonNull final QueueId queueId, @NonNull final PointerType pointerType, @NonNull final Long value) {
        final Statement insert = QueryBuilder.insertInto(Tables.Pointer.TABLE_NAME)
                                             .ifNotExists()
                                             .value(Tables.Pointer.VALUE, value)
                                             .value(Tables.Pointer.QUEUE_ID, queueId.get())
                                             .value(Tables.Pointer.POINTER_TYPE, pointerType.toString());

        session.execute(insert);
    }

    @Override
    public boolean queueExists(@NonNull final QueueName queueName) {
        return getActiveQueue(queueName).isPresent();
    }

    @Override
    public Optional<QueueDefinition> getQueue(@NonNull final QueueId queueId) {
        final Select.Where queryOne =
                QueryBuilder.select().all()
                            .from(Tables.Queue.TABLE_NAME)
                            .where(eq(Tables.Queue.QUEUE_ID, queueId.get()));

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
        final Statement highestVersion = QueryBuilder.select(Tables.QueueNameManager.VERSION)
                                                     .from(Tables.QueueNameManager.TABLE_NAME)
                                                     .where(eq(Tables.QueueNameManager.QUEUE_NAME, name.get()));

        final Row result = session.execute(highestVersion).one();

        if (result == null) {
            return Optional.empty();
        }

        final int activeVersion = result.getInt(Tables.QueueNameManager.VERSION);

        final Optional<QueueDefinition> queue = getQueue(QueueId.valueOf(name, activeVersion));

        if (queue.isPresent() && queue.get().getStatus() == QueueStatus.Active) {
            return queue;
        }

        return Optional.empty();
    }

    @Override
    public boolean tryDeleteQueueDefinition(@NonNull final QueueDefinition definition) {
        final Update.Conditions updateTrackingTable =
                QueryBuilder.update(Tables.QueueNameManager.TABLE_NAME)
                            .where(eq(Tables.QueueNameManager.QUEUE_NAME, definition.getQueueName().get()))
                            .with(set(Tables.QueueNameManager.STATUS, QueueStatus.Inactive.ordinal()))
                            .onlyIf(eq(Tables.QueueNameManager.VERSION, definition.getVersion()))
                            .and(eq(Tables.QueueNameManager.STATUS, QueueStatus.Active.ordinal()));

        if (!session.execute(updateTrackingTable).wasApplied()) {
            return false;
        }

        Statement delete = QueryBuilder.delete().all()
                                       .from(Tables.Queue.TABLE_NAME)
                                       .where(eq(Tables.Queue.QUEUE_ID, definition.getId().get()));

        return session.execute(delete).wasApplied();
    }
}
