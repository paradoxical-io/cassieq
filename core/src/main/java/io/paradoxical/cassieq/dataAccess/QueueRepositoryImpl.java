package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
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
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static java.util.stream.Collectors.toList;

public class QueueRepositoryImpl extends RepositoryBase implements QueueRepository {

    private final Session session;

    @Inject
    public QueueRepositoryImpl(@NonNull final Session session) {
        this.session = session;
    }

    @Override
    public void createQueue(@NonNull final QueueDefinition definition) throws QueueExistsError {
        insertQueueRecord(definition);

        initializeMonotonicValue(definition.getId());

        initializePointers(definition.getId());
    }

    @Override
    public void setQueueStatus(@NonNull final QueueId queueId, @NonNull final QueueStatus status) {
        final Statement update = QueryBuilder.update(Tables.Queue.TABLE_NAME)
                                             .where(eq(Tables.Queue.QUEUE_ID, queueId.get()))
                                             .with(set(Tables.Queue.STATUS, status.name()));

        session.execute(update);
    }

    private void insertQueueRecord(@NonNull final QueueDefinition queueDefinition) throws QueueExistsError {
        // if an active queue exists you can't create another one of the same name
        if (getActiveQueue(queueDefinition.getQueueName()).isPresent()) {
            throw new QueueExistsError(queueDefinition);
        }

        // if however, an inactve queue exists, make sure only one person can
        // create the next active queue (prevent race conditinos of multiple queues being created
        // of the same name that are active
        // first check the table name version table
        final Select.Where activeQueueIdSelect = QueryBuilder.select()
                                                             .column(Tables.QueueIdIndex.VERSION)
                                                             .from(Tables.QueueIdIndex.TABLE_NAME)
                                                             .where(eq(Tables.QueueIdIndex.QUEUE_NAME, queueDefinition.getQueueName().get()));

        final Row row = session.execute(activeQueueIdSelect).one();

        int currentVersion;

        if (row == null) {
            currentVersion = insertQueueIndexTracking(queueDefinition);
        }
        else {
            currentVersion = session.execute(activeQueueIdSelect).one().getInt(0);
        }

        final int nextVersion = currentVersion + 1;

        final Statement insertTrackingId = QueryBuilder.insertInto(Tables.QueueIdIndex.TABLE_NAME)
                                                       .ifNotExists()
                                                       .value(Tables.QueueIdIndex.QUEUE_NAME, queueDefinition.getQueueName().get())
                                                       .value(Tables.QueueIdIndex.VERSION, nextVersion);

        if (!session.execute(insertTrackingId).wasApplied()) {
            throw new QueueExistsError(queueDefinition);
        }

        final QueueId queueId = QueueId.valueOf(queueDefinition.getQueueName(), nextVersion);

        final Insert insertQueue = QueryBuilder.insertInto(Tables.Queue.TABLE_NAME)
                                               .ifNotExists()
                                               .value(Tables.Queue.QUEUE_ID, queueId.get())
                                               .value(Tables.Queue.BUCKET_SIZE, queueDefinition.getBucketSize().get())
                                               .value(Tables.Queue.QUEUE_NAME, queueDefinition.getQueueName().get())
                                               .value(Tables.Queue.MAX_DELIVERY_COUNT, queueDefinition.getMaxDeliveryCount())
                                               .value(Tables.Queue.STATUS, QueueStatus.Active.name());

        session.execute(insertQueue);

        queueDefinition.setId(queueId);
    }

    /**
     * Inserts the queue definition into the tracking table if no queue with this name was ever made
     *
     * @param queueDefinition
     */
    private int insertQueueIndexTracking(@NonNull final QueueDefinition queueDefinition) throws QueueExistsError {

        final int initialVersion = 0;

        final Statement value = QueryBuilder.insertInto(Tables.QueueIdIndex.TABLE_NAME)
                                            .ifNotExists()
                                            .value(Tables.QueueIdIndex.QUEUE_NAME, queueDefinition.getQueueName().get())
                                            .value(Tables.QueueIdIndex.VERSION, initialVersion);


        if (!session.execute(value).wasApplied()) {
            throw new QueueExistsError(queueDefinition);
        }

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
        final Select limit = QueryBuilder.select(Tables.QueueIdIndex.VERSION)
                                         .from(Tables.QueueIdIndex.TABLE_NAME)
                                         .where(eq(Tables.QueueIdIndex.QUEUE_NAME, name.get()))
                                         .limit(1);

        final Row result = session.execute(limit).one();

        if (result == null) {
            return Optional.empty();
        }

        final int activeVersion = result.getInt(Tables.QueueIdIndex.VERSION);

        final Optional<QueueDefinition> queue = getQueue(QueueId.valueOf(name, activeVersion));

        if (queue.isPresent() && queue.get().getStatus() == QueueStatus.Active) {
            return queue;
        }

        return Optional.empty();
    }

    @Override
    public void deleteQueueDefinition(@NonNull final QueueDefinition definition) {
        Statement delete = QueryBuilder.delete().all()
                                       .from(Tables.Queue.TABLE_NAME)
                                       .where(eq(Tables.Queue.QUEUE_ID, definition.getId().get()));

        session.execute(delete);
    }
}
