package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageRepository;
import io.paradoxical.cassieq.model.BucketPointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MessagePointer;
import io.paradoxical.cassieq.model.MessageTag;
import io.paradoxical.cassieq.model.MessageUpdateRequest;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import io.paradoxical.cassieq.model.time.Clock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.incr;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toList;

public class MessageRepositoryImpl extends RepositoryBase implements MessageRepository {
    private Logger logger = getLogger(MessageRepositoryImpl.class);

    private final Session session;
    private final Clock clock;
    private final QueueDefinition queueDefinition;

    @Inject
    public MessageRepositoryImpl(
            Provider<Session> session,
            Clock clock,
            @Assisted QueueDefinition queueDefinition) {
        this.session = session.get();
        this.clock = clock;
        this.queueDefinition = queueDefinition;

        logger = logger.with("queue-name", queueDefinition.getQueueName())
                       .with("version", queueDefinition.getVersion());
    }

    @Override
    public void putMessage(final Message message, final Duration initialInvisibility) throws ExistingMonotonFoundException {
        final DateTime now = getNow();
        final Long bucketPointer = message.getIndex().toBucketPointer(queueDefinition.getBucketSize()).get();

        final MessageTag randomMessageTag = MessageTag.random();

        Statement statement = QueryBuilder.insertInto(Tables.Message.TABLE_NAME)
                                          .ifNotExists()
                                          .value(Tables.Message.QUEUE_ID, queueDefinition.getId().get())
                                          .value(Tables.Message.BUCKET_NUM, bucketPointer)
                                          .value(Tables.Message.MONOTON, message.getIndex().get())
                                          .value(Tables.Message.VERSION, 1)
                                          .value(Tables.Message.DELIVERY_COUNT, 0)
                                          .value(Tables.Message.ACKED, false)
                                          .value(Tables.Message.MESSAGE, message.getBlob())
                                          .value(Tables.Message.NEXT_VISIBLE_ON, now.plus(initialInvisibility).toDate())
                                          .value(Tables.Message.CREATED_DATE, now.toDate())
                                          .value(Tables.Message.TAG, randomMessageTag.get());

        final boolean wasInserted = session.execute(statement).wasApplied();

        if (!wasInserted) {
            throw new ExistingMonotonFoundException(String.format("Tried to insert a message with the monoton value of '%s' which already exists", message.getIndex()));
        }

        updateQueueSize(1);
    }


    public Optional<Message> rawConsumeMessage(final Message message, final Duration duration) {

        final Long bucketPointer = message.getIndex().toBucketPointer(queueDefinition.getBucketSize()).get();

        /*
            Three invariants when consuming: invis time, version, and delivery count are always bumped atomically
         */
        final DateTime newInvisTime = getNow().plus(duration);
        final int newVersion = message.getVersion() + 1;
        final int deliveryCount = message.getDeliveryCount() + 1;

        final Statement statement = QueryBuilder.update(Tables.Message.TABLE_NAME)
                                                .with(set(Tables.Message.NEXT_VISIBLE_ON, newInvisTime.toDate()))
                                                .and(set(Tables.Message.VERSION, newVersion))
                                                .and(set(Tables.Message.DELIVERY_COUNT, deliveryCount))
                                                .where(eq(Tables.Message.QUEUE_ID, queueDefinition.getId().get()))
                                                .and(eq(Tables.Message.BUCKET_NUM, bucketPointer))
                                                .and(eq(Tables.Message.MONOTON, message.getIndex().get()))
                                                .onlyIf(eq(Tables.Message.VERSION, message.getVersion()))
                                                .and(eq(Tables.Message.ACKED, false));

        if (session.execute(statement).wasApplied()) {
            return Optional.of(message.withNewVersion(newVersion));
        }

        return Optional.empty();
    }

    private DateTime getNow() {
        return clock.now().toDateTime(DateTimeZone.UTC);
    }

    @Override
    public boolean ackMessage(final Message message) {
        // conditionally ack if message version is the same as in the message
        //  if was able to update then return true, otehrwise false

        final Long bucketPointer = message.getIndex().toBucketPointer(queueDefinition.getBucketSize()).get();

        Statement statement = QueryBuilder.update(Tables.Message.TABLE_NAME)
                                          .with(set(Tables.Message.ACKED, true))
                                          .and(set(Tables.Message.VERSION, message.getVersion() + 1))
                                          .where(eq(Tables.Message.QUEUE_ID, queueDefinition.getId().get()))
                                          .and(eq(Tables.Message.BUCKET_NUM, bucketPointer))
                                          .and(eq(Tables.Message.MONOTON, message.getIndex().get()))
                                          .onlyIf(eq(Tables.Message.VERSION, message.getVersion()));

        final ResultSet resultSet = session.execute(statement);

        final boolean wasApplied = resultSet.wasApplied();

        if(wasApplied) {
            updateQueueSize(-1);
        }

        return wasApplied;
    }

    @Override
    public boolean finalize(final RepairBucketPointer bucketPointer) {
        return insertSpecialIndex(SpecialIndex.Finalizer, bucketPointer);
    }

    @Override
    public boolean tombstone(final ReaderBucketPointer bucketPointer) {
        return insertSpecialIndex(SpecialIndex.Tombstone, bucketPointer);
    }

    @Override
    public List<Message> getBucketContents(final BucketPointer bucketPointer) {
        // list all messages in bucket
        Statement query = getReadMessageQuery(bucketPointer);

        return session.execute(query)
                      .all()
                      .stream()
                      .map(Message::fromRow)
                      .collect(toList());
    }

    @Override
    public Optional<DateTime> tombstoneExists(final BucketPointer bucketPointer) {
        Statement query = getReadMessageQuery(bucketPointer).and(eq(Tables.Message.MONOTON, SpecialIndex.Tombstone.getIndex().get()));

        return Optional.ofNullable(getOne(session.execute(query), row -> new DateTime(row.getDate(Tables.Message.CREATED_DATE))));
    }

    @Override
    public void deleteAllMessages(final BucketPointer bucket) {
        final Statement delete = QueryBuilder.delete()
                                             .all()
                                             .from(Tables.Message.TABLE_NAME)
                                             .where(eq(Tables.Message.QUEUE_ID, queueDefinition.getId().get()))
                                             .and(eq(Tables.Message.BUCKET_NUM, bucket.get()));

        session.execute(delete);
    }

    @Override
    public Message getMessage(final MessagePointer pointer) {
        final BucketPointer bucketPointer = ReaderBucketPointer.valueOf(pointer.get() / queueDefinition.getBucketSize().get());

        Statement query = getReadMessageQuery(bucketPointer).and(eq(Tables.Message.MONOTON, pointer.get()));

        return getOne(session.execute(query), Message::fromRow);
    }

    @Override
    public Optional<Message> updateMessage(MessageUpdateRequest message) {
        final DateTime now = getNow();

        final Date nextVisibleOn = now.plus(message.getInvisibilityDuration()).toDate();

        final Update.Assignments updater =
                QueryBuilder.update(Tables.Message.TABLE_NAME)
                            .where(eq(Tables.Message.QUEUE_ID, queueDefinition.getId().get()))
                            .and(eq(Tables.Message.BUCKET_NUM, message.getIndex().toBucketPointer(queueDefinition.getBucketSize()).get()))
                            .and(eq(Tables.Message.MONOTON, message.getIndex().get()))
                            .with(set(Tables.Message.VERSION, message.getVersion() + 1))
                            .and(set(Tables.Message.NEXT_VISIBLE_ON, nextVisibleOn));

        if (message.getNewBlob() != null) {
            updater.and(set(Tables.Message.MESSAGE, message.getNewBlob()));
        }

        updater.onlyIf(eq(Tables.Message.VERSION, message.getVersion()))
               .and(eq(Tables.Message.TAG, message.getTag().get()))
               .with(set(Tables.Message.UPDATED_DATE, now.toDate()));

        if (session.execute(updater).wasApplied()) {
            logger.with("monoton", message.getIndex())
                  .with("tag", message.getTag())
                  .with("next-visible-on", nextVisibleOn)
                  .debug("Updating message");

            return Optional.of(getMessage(message.getIndex()));
        }

        return Optional.empty();
    }

    @Override
    public boolean finalizedExists(final BucketPointer bucketPointer) {
        Statement query = getReadMessageQuery(bucketPointer).and(eq(Tables.Message.MONOTON, SpecialIndex.Finalizer.getIndex().get()));

        return Optional.ofNullable(getOne(session.execute(query), row -> true)).orElse(false);
    }

    private Select.Where getReadMessageQuery(final BucketPointer bucketPointer) {
        return QueryBuilder.select()
                           .all()
                           .from(Tables.Message.TABLE_NAME)
                           .where(eq(Tables.Message.QUEUE_ID, queueDefinition.getId().get()))
                           .and(eq(Tables.Message.BUCKET_NUM, bucketPointer.get()));
    }

    private boolean insertSpecialIndex(SpecialIndex specialIndex, BucketPointer bucketPointer) {
        final DateTime now = getNow();

        Statement statement = QueryBuilder.insertInto(Tables.Message.TABLE_NAME)
                                          .ifNotExists()
                                          .value(Tables.Message.QUEUE_ID, queueDefinition.getId().get())
                                          .value(Tables.Message.BUCKET_NUM, bucketPointer.get())
                                          .value(Tables.Message.ACKED, true)
                                          .value(Tables.Message.MONOTON, specialIndex.getIndex().get())
                                          .value(Tables.Message.NEXT_VISIBLE_ON, now.toDate())
                                          .value(Tables.Message.CREATED_DATE, now.toDate());

        return session.execute(statement).wasApplied();
    }

    private void updateQueueSize(int amount){
        final Statement with = QueryBuilder.update(Tables.QueueStats.TABLE_NAME)
                                           .where(eq(Tables.QueueStats.QUEUE_STATS_ID, queueDefinition.getQueueStatsId().get()))
                                           .with(incr(Tables.QueueStats.SIZE, amount));

        session.execute(with);
    }
}
