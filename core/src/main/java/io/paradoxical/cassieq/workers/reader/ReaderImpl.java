package io.paradoxical.cassieq.workers.reader;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.BucketPointer;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MessagePointer;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.PopReceipt;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.time.Clock;
import org.joda.time.Duration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.godaddy.logging.LoggerFactory.getLogger;

/**
 * Invis pointer algo:
 *
 * if a message is available for consumption (never consumed)
 *
 * Story time!
 *
 * Imagine this scenario:
 *
 * ~   = out for consumption
 * INV = message is invisible
 * = location of inivs pointer
 * T   = tombstoned
 * +   = at least once delivered
 * A   = acked
 * --  = bucket line
 *
 * Message Id | Status
 *
 * 0 A *
 * 1
 * 2
 * --
 * 3
 *
 * Zero is acked. 1, 2 and 3. Two reads come in at the same time.  Both try and claim 1,
 * but only 1 of the consumers gets in, so the failed consumer (due to version changes)
 * retries and gets message 2.  Invis pointer is still on zero, since it can't move past
 * never delivered messages and is only moved on read begin.
 *
 * 0 A *
 * 1 ~ INV - DEAD
 * 2 ~ INV
 * --
 * 3
 *
 * Lets say now that message 2 is acked
 *
 * 0 A *
 * 1 ~ INV - DEAD
 * 2 A
 * --
 * 3
 *
 * Now two more reads come in and message 1 is ready for redelivery since its alive again
 *
 * At this point, the invis pointer finds message 1 and sits on it. It gets returned as the message to consume
 * since its alive again, its visiblity gets updated to next, and the invis pointer parks.
 *
 * 0 A
 * 1 + *
 * 2 A
 * --
 * 3
 *
 * Now message 1 is acked, invis pointer stays put. The next read comes in, invis pointer moves to 3
 * and parks since its not allowed to advance past never delivered messages
 *
 * 0 A
 * 1 A
 * 2 A
 * --
 * 3 *
 */
public class ReaderImpl implements Reader {
    private Logger logger = getLogger(ReaderImpl.class);

    private final DataContext dataContext;
    private final QueueRepository queueRepository;
    private final Clock clock;
    private final QueueDefinition queueDefinition;

    @Inject
    public ReaderImpl(
            DataContextFactory dataContextFactory,
            QueueRepository queueRepository,
            Clock clock,
            @Assisted QueueDefinition queueDefinition) {
        this.queueRepository = queueRepository;
        this.clock = clock;
        this.queueDefinition = queueDefinition;
        dataContext = dataContextFactory.forQueue(queueDefinition);

        logger = logger.with("q", queueDefinition.getQueueName()).with("version", queueDefinition.getVersion());
    }

    @Override
    public Optional<Message> nextMessage(Duration invisibility) {
        if (!isActive()) {
            return Optional.empty();
        }

        final Optional<Message> nowVisibleMessage = tryGetNowVisibleMessage(getCurrentInvisPointer(), invisibility);

        if (nowVisibleMessage.isPresent()) {

            logger.with(nowVisibleMessage.get()).info("Got newly visible message");

            return nowVisibleMessage;
        }

        final Optional<Message> nextMessage = getAndMark(getReaderCurrentBucket(), invisibility);

        if (nextMessage.isPresent()) {
            logger.with(nextMessage.get()).info("Got message");
        }

        return nextMessage;
    }

    private boolean isActive() {
        return queueRepository.getActiveQueue(queueDefinition.getQueueName()).isPresent();
    }

    @Override
    public boolean ackMessage(final PopReceipt popReceipt) {
        final Message messageAt = dataContext.getMessageRepository().getMessage(popReceipt.getMessageIndex());

        if (messageAt.getVersion() != popReceipt.getMessageVersion() ||
            !messageAt.getTag().equals(popReceipt.getMessageTag())) {
            return false;
        }

        return dataContext.getMessageRepository().ackMessage(messageAt);
    }

    private InvisibilityMessagePointer getCurrentInvisPointer() {
        return dataContext.getPointerRepository().getCurrentInvisPointer();
    }

    private ReaderBucketPointer getReaderCurrentBucket() {
        return dataContext.getPointerRepository().getReaderCurrentBucket();
    }

    private Optional<Message> tryGetNowVisibleMessage(InvisibilityMessagePointer pointer, Duration invisiblity) {
        final Message messageAt = dataContext.getMessageRepository().getMessage(pointer);

        if (messageAt == null) {
            // invis pointer points to garbage, try and find something else
            return findNextInvisMessage(pointer, invisiblity);
        }

        if (messageAt.getDeliveryCount() == 0) {
            // it hasn't been sent out for delivery yet so can't be invisible
            return Optional.empty();
        }

        if (messageAt.isVisible(clock) && messageAt.isNotAcked()) {
            // the message has come back alive
            final Optional<Message> message = dataContext.getMessageRepository().consumeMessage(messageAt, invisiblity);

            // were able to consume
            if (message.isPresent()) {
                return message;
            }

            return findNextInvisMessage(pointer, invisiblity);
        }
        else if (messageAt.isAcked()) {
            // current message is acked that the invis pointer was pointing to
            // try and move the invis pointer to the next lowest monotonic invisible
            return findNextInvisMessage(pointer, invisiblity);
        }

        return Optional.empty();
    }

    private Optional<Message> findNextInvisMessage(final InvisibilityMessagePointer pointer, Duration invisiblity) {
        // check all the messages in the bucket the invis pointer is currently on
        final ReaderBucketPointer bucketPointer = pointer.toBucketPointer(queueDefinition.getBucketSize());

        final List<Message> messages = dataContext.getMessageRepository()
                                                  .getMessages(bucketPointer);

        final BucketPointer maxMonotonBucketPointer = dataContext.getMonotonicRepository().getCurrent().toBucketPointer(queueDefinition.getBucketSize());

        final BucketPointer invisBucketPointer = pointer.toBucketPointer(queueDefinition.getBucketSize());

        if (messages.isEmpty() && invisBucketPointer.get() >= maxMonotonBucketPointer.get()) {
            // no messages, and we're at the last bucket anyways, can't move pointer since nothing to move to
            return Optional.empty();
        }

        // in the active bucket, if there is a not acked, is currenlty invisible, and has been at least once delivered message
        // then if its ID is LESS than the current active pointer, move the pointer to that
        // otherwise pointer stays the same
        final Optional<Message> stoppingPoint = messages.stream()
                                                        .filter(m -> m.isNotAcked() &&
                                                                     m.isNotVisible(clock) &&
                                                                     m.getDeliveryCount() > 0).findFirst();

        if (stoppingPoint.isPresent()) {
            trySetNewInvisPointer(pointer, stoppingPoint.get().getIndex());

            logger.with(stoppingPoint.get()).info("Found invis message in current bucket");

            return Optional.empty();
        }

        // since we're scanning, look for a message that has been delivered
        // is visible, and is not acked. this guy is now alive
        final Optional<Message> nextNewlyAlive = messages.stream()
                                                         .filter(m -> m.isNotAcked() && m.isVisible(clock) && m.getDeliveryCount() > 0)
                                                         .findFirst();

        if (nextNewlyAlive.isPresent()) {
            // stop here since we're scanning and we can't have anyone else behind us
            // since they are either invis, OR they are acked
            // attempt to consume the message
            final Optional<Message> tryConsumedMessage = dataContext.getMessageRepository().consumeMessage(nextNewlyAlive.get(), invisiblity);

            // if we were able to consume the message, try and move the invis pointer to this isnce its going to now be invis.
            // if someone else finds an earlier invis, it'll get moved to that
            if(tryConsumedMessage.isPresent()){
                trySetNewInvisPointer(pointer, nextNewlyAlive.get().getIndex());

                return tryConsumedMessage;
            }
        }

        InvisibilityMessagePointer pointerForNextBucket = getPointerForNextBucket(pointer);

        return tryGetNowVisibleMessage(pointerForNextBucket, invisiblity);
    }


    /**
     * Given the current pointer, returns a new pointer that jumps to the start of the next bucket
     *
     * @param pointer
     * @return
     */
    private InvisibilityMessagePointer getPointerForNextBucket(InvisibilityMessagePointer pointer) {
        final ReaderBucketPointer bucketPointer = pointer.toBucketPointer(queueDefinition.getBucketSize());

        final MonotonicIndex monotonicIndex = bucketPointer.next().startOf(queueDefinition.getBucketSize());

        return InvisibilityMessagePointer.valueOf(monotonicIndex);
    }

    private Optional<Message> getAndMark(ReaderBucketPointer currentBucket, Duration invisiblity) {

        while (true) {
            final List<Message> allMessages = dataContext.getMessageRepository().getMessages(currentBucket);

            final boolean allComplete = allMessages.stream().allMatch(m -> m.isAcked() || m.isNotVisible(clock));

            if (allComplete) {
                if (allMessages.size() == queueDefinition.getBucketSize().get() || monotonPastBucket(currentBucket)) {
                    tombstone(currentBucket);

                    currentBucket = advanceBucket(currentBucket);

                    continue;
                }
                else {
                    // bucket not ready to be closed yet, but all current messages processed
                    return Optional.empty();
                }
            }

            final Optional<Message> foundMessage = allMessages.stream().filter(m -> m.isNotAcked() && m.isVisible(clock)).findFirst();

            if (!foundMessage.isPresent()) {
                return Optional.empty();
            }

            final Message visibleMessage = foundMessage.get();

            final Optional<Message> consumedMessage = dataContext.getMessageRepository().consumeMessage(visibleMessage, invisiblity);

            if (!consumedMessage.isPresent()) {
                // someone else did it, fuck it, try again for the next visibleMessage
                logger.with(visibleMessage).trace("Someone else consumed the visibleMessage!");

                continue;
            }

            return consumedMessage;
        }
    }

    private void tombstone(final ReaderBucketPointer bucket) {
        if (dataContext.getMessageRepository().tombstone(bucket)) {
            logger.with(bucket).info("Tombestoned reader");
        }
    }

    private boolean monotonPastBucket(final ReaderBucketPointer currentBucket) {
        final BucketPointer currentMonotonicBucket = getLatestMonotonic().toBucketPointer(queueDefinition.getBucketSize());

        return currentMonotonicBucket.get() > currentBucket.get();
    }

    private ReaderBucketPointer advanceBucket(ReaderBucketPointer currentBucket) {
        final ReaderBucketPointer nextBucket = dataContext.getPointerRepository().advanceMessageBucketPointer(currentBucket, currentBucket.next());

        if (!Objects.equals(nextBucket.get(), currentBucket.get())) {
            logger.with(currentBucket).info("Advancing reader bucket");
        }

        return nextBucket;
    }

    private MonotonicIndex getLatestMonotonic() {
        return dataContext.getMonotonicRepository().getCurrent();
    }

    private InvisibilityMessagePointer trySetNewInvisPointer(final InvisibilityMessagePointer currentInvis, MessagePointer potentialNextInvisPointer) {
        return dataContext.getPointerRepository().tryMoveInvisiblityPointerTo(currentInvis, InvisibilityMessagePointer.valueOf(potentialNextInvisPointer.get()));
    }
}
