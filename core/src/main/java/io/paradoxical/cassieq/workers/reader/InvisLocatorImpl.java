package io.paradoxical.cassieq.workers.reader;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
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
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.time.Clock;
import lombok.Cleanup;
import org.joda.time.Duration;

import java.util.List;
import java.util.Optional;

import static com.codahale.metrics.MetricRegistry.name;
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

public class InvisLocatorImpl implements InvisLocator {
    private final QueueRepository queueRepository;
    private final Clock clock;
    private final MetricRegistry metricRegistry;
    private final QueueDefinition queueDefinition;
    private Logger logger = getLogger(InvisLocatorImpl.class);
    private final DataContext dataContext;


    @Inject
    public InvisLocatorImpl(
            DataContextFactory dataContextFactory,
            QueueRepository queueRepository,
            Clock clock,
            MetricRegistry metricRegistry,
            @Assisted QueueDefinition queueDefinition) {
        this.queueRepository = queueRepository;
        this.clock = clock;
        this.metricRegistry = metricRegistry;
        this.queueDefinition = queueDefinition;
        this.dataContext = dataContextFactory.forQueue(queueDefinition);

        logger = logger.with("q", queueDefinition.getQueueName()).with("version", queueDefinition.getVersion());
    }


    public Optional<Message> tryConsumeNextVisibleMessage(InvisibilityMessagePointer pointer, Duration invisiblity) {
        @Cleanup("close")
        @SuppressWarnings("unused")
        final Timer.Context timer = metricRegistry.timer(name("reader", "invisibility", "try-consume")).time();

        final Message messageAt = dataContext.getMessageRepository().getMessage(pointer);

        if (messageAt == null) {
            // invis pointer points to garbage, try and find something else
            return findAndConsumeNextVisible(pointer, invisiblity);
        }

        if (messageAt.getDeliveryCount() == 0) {
            // it hasn't been sent out for delivery yet so can't be invisible
            // dont ever move past non delivered messages with invis
            return Optional.empty();
        }

        if (messageAt.isVisible(clock) && messageAt.isNotAcked()) {
            // the message we are pointing at has come back alive
            final Optional<Message> message = dataContext.getMessageRepository().consumeMessage(messageAt, invisiblity);

            // were able to consume
            if (message.isPresent()) {
                metricRegistry.counter(name("reader", "revived", "messages")).inc();

                return message;
            }

            // scan for the next visible if we have one
            return findAndConsumeNextVisible(pointer, invisiblity);
        }
        else if (messageAt.isAcked()) {
            // current message is acked that the invis pointer was pointing to
            // try and move the invis pointer to the next lowest monotonic invisible
            return findAndConsumeNextVisible(pointer, invisiblity);
        }

        return Optional.empty();
    }

    /**
     * Either find an invis message to stop at, or look for messages that _were_ invis but
     * now have come back alive.
     *
     * @param pointer
     * @param invisiblity
     * @return
     */
    private Optional<Message> findAndConsumeNextVisible(final InvisibilityMessagePointer pointer, Duration invisiblity) {
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

        if (stopAtInvisMessageInBucket(messages, pointer)) {
            return Optional.empty();
        }

        final Optional<Message> newlyAliveMessage = consumeNewlyAliveMessageInBucket(messages, invisiblity, pointer);

        if (newlyAliveMessage.isPresent()) {
            return newlyAliveMessage;
        }

        return tryConsumeNextVisibleMessage(getPointerForNextBucket(pointer), invisiblity);
    }

    /**
     * in the active bucket, if there is a not acked, is currenlty invisible, and has been at least once delivered message
     * then if its ID is LESS than the current active pointer, move the pointer to that
     * otherwise pointer stays the same
     **/
    private boolean stopAtInvisMessageInBucket(final List<Message> messages, final InvisibilityMessagePointer pointer) {

        final Optional<Message> stoppingPoint = messages.stream()
                                                        .filter(m -> m.isNotAcked() &&
                                                                     m.isNotVisible(clock) &&
                                                                     m.getDeliveryCount() > 0).findFirst();

        if (stoppingPoint.isPresent()) {
            trySetNewInvisPointer(pointer, stoppingPoint.get().getIndex());

            logger.with(stoppingPoint.get()).info("Found invis message in current bucket");

            return true;
        }

        return false;
    }

    /**
     * Look in this message bucket list and find a message that has now become alive and consume it
     *
     * @param messagesInBucket
     * @param invisiblity
     * @param currentPointer
     * @return
     */
    private Optional<Message> consumeNewlyAliveMessageInBucket(
            final List<Message> messagesInBucket,
            final Duration invisiblity,
            InvisibilityMessagePointer currentPointer) {
        // since we're scanning, look for a message that has been delivered
        // is visible, and is not acked. this guy is now alive
        final Optional<Message> nextNewlyAlive = messagesInBucket.stream()
                                                                 .filter(m -> m.isNotAcked() && m.isVisible(clock) && m.getDeliveryCount() > 0)
                                                                 .findFirst();

        if (nextNewlyAlive.isPresent()) {
            // stop here since we're scanning and we can't have anyone else behind us
            // since they are either invis, OR they are acked
            // attempt to consume the message
            final Optional<Message> tryConsumedMessage = dataContext.getMessageRepository().consumeMessage(nextNewlyAlive.get(), invisiblity);

            // if we were able to consume the message, try and move the invis pointer to this isnce its going to now be invis.
            // if someone else finds an earlier invis, it'll get moved to that
            if (tryConsumedMessage.isPresent()) {
                trySetNewInvisPointer(currentPointer, nextNewlyAlive.get().getIndex());

                return tryConsumedMessage;
            }
        }

        return Optional.empty();
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

    private InvisibilityMessagePointer trySetNewInvisPointer(final InvisibilityMessagePointer currentInvis, MessagePointer potentialNextInvisPointer) {
        return dataContext.getPointerRepository().tryMoveInvisiblityPointerTo(currentInvis, InvisibilityMessagePointer.valueOf(potentialNextInvisPointer.get()));
    }
}
