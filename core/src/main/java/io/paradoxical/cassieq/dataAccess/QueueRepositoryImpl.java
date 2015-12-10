package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.PointerType;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;

import java.util.List;
import java.util.Optional;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static java.util.stream.Collectors.toList;

public class QueueRepositoryImpl extends RepositoryBase implements QueueRepository {

    private final Session session;

    @Inject
    public QueueRepositoryImpl(final Session session) {
        this.session = session;
    }

    @Override
    public void createQueue(final QueueDefinition definition) {

        initializeMonotonicValue(definition.getQueueName());

        initializePointers(definition.getQueueName());

        insertQueueRecord(definition);
    }

    @Override
    public void setQueueStatus(final QueueDefinition queueDefinition, final QueueStatus status) {
        final Statement update = QueryBuilder.update(Tables.Queue.TABLE_NAME)
                                             .where(eq(Tables.Queue.QUEUENAME, queueDefinition.getQueueName().get()))
                                             .with(set(Tables.Queue.STATUS, status.name()));

        session.execute(update);
    }

    private void insertQueueRecord(final QueueDefinition queueDefinition) {
        final Insert insertQueue = QueryBuilder.insertInto(Tables.Queue.TABLE_NAME)
                                               .ifNotExists()
                                               .value(Tables.Queue.QUEUENAME, queueDefinition.getQueueName().get())
                                               .value(Tables.Queue.BUCKET_SIZE, queueDefinition.getBucketSize().get())
                                               .value(Tables.Queue.MAX_DEQUEUE_COUNT, queueDefinition.getMaxDeliveryCount())
                                               .value(Tables.Queue.STATUS, QueueStatus.Active.name());

        session.execute(insertQueue);
    }

    private void initializePointers(final QueueName queueName) {

        initializePointer(queueName, PointerType.BUCKET_POINTER, 0L);
        initializePointer(queueName, PointerType.INVISIBILITY_POINTER, -1L);
        initializePointer(queueName, PointerType.REPAIR_BUCKET, 0L);
    }

    private void initializeMonotonicValue(final QueueName queueName) {
        Statement statement = QueryBuilder.insertInto(Tables.Monoton.TABLE_NAME)
                                          .value(Tables.Monoton.QUEUENAME, queueName.get())
                                          .value(Tables.Monoton.VALUE, 0L)
                                          .ifNotExists();

        session.execute(statement);
    }

    private void initializePointer(final QueueName queueName, final PointerType pointerType, final Long value) {
        final Statement insert = QueryBuilder.insertInto(Tables.Pointer.TABLE_NAME)
                                             .ifNotExists()
                                             .value(Tables.Pointer.VALUE, value)
                                             .value(Tables.Pointer.QUEUENAME, queueName.get())
                                             .value(Tables.Pointer.POINTER_TYPE, pointerType.toString());

        session.execute(insert);
    }

    @Override
    public boolean queueExists(final QueueName queueName) {
        final Select.Where queryOne =
                QueryBuilder.select().all().from(Tables.Queue.TABLE_NAME).where(eq(Tables.Queue.QUEUENAME, queueName.get()));

        return getOne(session.execute(queryOne), row -> true) != null;
    }

    @Override
    public Optional<QueueDefinition> getQueue(final QueueName queueName) {
        final Select.Where queryOne =
                QueryBuilder.select().all().from(Tables.Queue.TABLE_NAME).where(eq(Tables.Queue.QUEUENAME, queueName.get()));

        final QueueDefinition result = getOne(session.execute(queryOne), QueueDefinition::fromRow);
        return Optional.ofNullable(result);
    }

    @Override
    public List<QueueDefinition> getQueues() {
        final Select query = QueryBuilder.select().all().from(Tables.Queue.TABLE_NAME);

        return session.execute(query)
                      .all()
                      .stream()
                      .map(QueueDefinition::fromRow).collect(toList());
    }

    @Override
    public void deleteQueueDefinition(final QueueName queueName) {
        final Statement delete = QueryBuilder.delete().all()
                                             .from(Tables.Queue.TABLE_NAME)
                                             .where(eq(Tables.Queue.QUEUENAME, queueName.get()));

        session.execute(delete);
    }
}
