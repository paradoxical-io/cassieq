package io.paradoxical.cassieq.dataAccess.interfaces;

import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.model.BucketPointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MessagePointer;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public interface MessageRepository {
    void putMessage(final Message message, final Duration initialInvisibility) throws ExistingMonotonFoundException;

    default void putMessage(final Message message) throws ExistingMonotonFoundException {
        putMessage(message, Duration.ZERO);
    }

    Optional<Message> consumeMessage(final Message message, final Duration duration);

    boolean ackMessage(final Message message);

    default List<Message> getMessages(final BucketPointer bucketPointer) {
        return getBucketContents(bucketPointer).stream().filter(Message::isNotTombstone).collect(toList());
    }

    List<Message> getBucketContents(final BucketPointer bucketPointer);

    void tombstone(final ReaderBucketPointer bucketPointer);

    Message getMessage(final MessagePointer pointer);

    Optional<DateTime> tombstoneExists(final BucketPointer bucketPointer);

    void deleteAllMessages(BucketPointer bucket);
}