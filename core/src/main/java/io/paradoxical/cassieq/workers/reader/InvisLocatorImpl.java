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
import lombok.Data;
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

@Data
class InvisBucketProcessResult {
    private final Optional<Message> message;

    private final BucketScanResultAction resultAction;

    public static InvisBucketProcessResult nextBucket() {
        return new InvisBucketProcessResult(Optional.empty(), BucketScanResultAction.NextBucket);
    }

    public static InvisBucketProcessResult of(Optional<Message> message) {
        return new InvisBucketProcessResult(message, BucketScanResultAction.Stop);
    }
}

@Data
class InvisMessagePointerProcessResult {
    private final Optional<Message> message;

    private final PointerEvaluateResultAction resultAction;

    public static InvisMessagePointerProcessResult scanBucket() {
        return new InvisMessagePointerProcessResult(Optional.empty(), PointerEvaluateResultAction.ScanBucket);
    }

    public static InvisMessagePointerProcessResult of(Optional<Message> message) {
        return new InvisMessagePointerProcessResult(message, PointerEvaluateResultAction.Stop);
    }
}

enum PointerEvaluateResultAction {
    ScanBucket,
    Stop
}

enum BucketScanResultAction {
    NextBucket,
    Stop
}

public class InvisLocatorImpl implements InvisLocator {
    private final QueueRepository queueRepository;
    private final Clock clock;
    private final MetricRegistry metricRegistry;
    private final QueueDefinition queueDefinition;
    private Logger logger = getLogger(InvisLocatorImpl.class);
    private final DataContext dataContext;
    private final ReaderBucketPointer currentReaderBucket;


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

        currentReaderBucket = dataContext.getPointerRepository().getReaderCurrentBucket();

        logger = logger.with("q", queueDefinition.getQueueName()).with("version", queueDefinition.getVersion());
    }

    @Override
    public Optional<Message> tryConsumeNextVisibleMessage(InvisibilityMessagePointer pointer, Duration invisiblity) {
        @Cleanup("close")
        @SuppressWarnings("unused")
        final Timer.Context timer = metricRegistry.timer(name("reader", "invisibility", "try-consume")).time();

        return findAndConsumeNextVisible(pointer, currentReaderBucket, invisiblity);
    }

    /**
     * Either find an invis message to stop at,
     *
     * OR look for messages that _were_ invis but now have come back alive.
     *
     * OR a message that had an initial invis and was skipped by the reader (delivery count 0)
     * and is now visible
     *
     * @param startingPointer
     * @param invisiblity
     * @return
     */
    private Optional<Message> findAndConsumeNextVisible(
            final InvisibilityMessagePointer startingPointer,
            final ReaderBucketPointer currentReaderBucketPointer,
            final Duration invisiblity) {

        InvisibilityMessagePointer activePointer = startingPointer;

        // unwind the recursion since this may take a bit
        while (true) {
            // try what the current pointer is on
            final InvisMessagePointerProcessResult invisMessagePointerProcessResult = validateMessageAtPointer(activePointer, invisiblity);

            switch (invisMessagePointerProcessResult.getResultAction()) {
                // if the current pointer is invalid or needs more scanning, check the bucket we're in
                case ScanBucket:
                    final InvisBucketProcessResult invisBucketProcessResult = processBucket(activePointer, currentReaderBucketPointer, invisiblity);

                    switch (invisBucketProcessResult.getResultAction()) {
                        // if the bucket we're in needs to scan, advance the pointer and try again
                        case NextBucket:
                            // however advancing the pointer may be reset to another active pointer elsewhere,
                            // so try and set the active pointer so we can re-test it
                            final Optional<InvisibilityMessagePointer> nextBucketStartPointer = tryAdvancePointerToNextBucket(activePointer);

                            // if we got a new pointer, move to the next bucket
                            if(nextBucketStartPointer.isPresent()){
                                activePointer = nextBucketStartPointer.get();
                                continue;
                            }

                            return Optional.empty();

                        case Stop:
                            // found something or were told to stop scanning, return
                            return invisBucketProcessResult.getMessage();
                    }
                case Stop:
                    // found something or were told to stop scanning, return
                    return invisMessagePointerProcessResult.getMessage();
            }
        }
    }

    private InvisMessagePointerProcessResult validateMessageAtPointer(InvisibilityMessagePointer pointer, Duration invisibility) {
        final Message messageAt = dataContext.getMessageRepository().getMessage(pointer);

        if (messageAt == null) {
            // invis pointer points to garbage, try and find something else
            return InvisMessagePointerProcessResult.scanBucket();
        }

        if (messageAt.isNotVisible(clock) && messageAt.isNotAcked()) {
            // not acked, not visible, invis poiter is still valid
            // do not advance
            return InvisMessagePointerProcessResult.of(Optional.empty());
        }

        if (messageAt.isVisible(clock) && messageAt.isNotAcked()) {
            // the message we are pointing at has come back alive
            final Optional<Message> message = dataContext.getMessageRepository().consumeMessage(messageAt, invisibility);

            // were able to consume
            if (message.isPresent()) {
                metricRegistry.counter(name("reader", "revived", "messages")).inc();

                return InvisMessagePointerProcessResult.of(message);
            }
        }

        // scan for the next visible if we have one
        return InvisMessagePointerProcessResult.scanBucket();
    }

    private InvisBucketProcessResult processBucket(
            final InvisibilityMessagePointer activePointer,
            final ReaderBucketPointer currentReaderBucketPointer,
            final Duration invisiblity) {
        // check all the messages in the bucket the invis pointer is currently on
        final BucketPointer invisBucketPointer = activePointer.toBucketPointer(queueDefinition.getBucketSize());

        final List<Message> messagesInBucket = dataContext.getMessageRepository()
                                                          .getMessages(invisBucketPointer);

        final BucketPointer maxMonotonBucketPointer = dataContext.getMonotonicRepository()
                                                                 .getCurrent()
                                                                 .toBucketPointer(queueDefinition.getBucketSize());

        // no messages
        if (messagesInBucket.isEmpty()) {
            // if we're at the last bucket anyways, can't move pointer since nothing to move to
            if( invisBucketPointer.get() >= currentReaderBucketPointer.get()) {
                return InvisBucketProcessResult.of(Optional.empty());
            }

            // otherwise the repair has cleaned up since, and we should move on
            return InvisBucketProcessResult.nextBucket();
        }

        // if the reader has moved past the current bucket, check
        // if there are messages that are alive and were skipped
        if (invisBucketPointer.get() < currentReaderBucketPointer.get()) {
            // in the bucket, see if there is a revived message
            final Optional<Message> newlyAliveMessage = consumeRevivedMessageInBucket(messagesInBucket, invisiblity, activePointer);

            if (newlyAliveMessage.isPresent()) {
                return InvisBucketProcessResult.of(newlyAliveMessage);
            }
        }

        // stop at the next invisible message in the bucket if there is one
        // regardless of delivery status
        if (stopAtInvisMessageInBucket(messagesInBucket, activePointer)) {
            return InvisBucketProcessResult.of(Optional.empty());
        }

        // if we found a never delivered message in the bucket
        // dont move past it. the normal reader flow will pick up
        // this message, we dont want to claim it
        if (stopAtNonDeliveredMessage(messagesInBucket, activePointer)) {
            return InvisBucketProcessResult.of(Optional.empty());
        }

        if (fullAndAcked(messagesInBucket) || finalizedAndAllAcked(messagesInBucket, invisBucketPointer)) {
            return InvisBucketProcessResult.nextBucket();
        }

        // not all messages are acked and the bucket isn't finalized yet
        return InvisBucketProcessResult.of(Optional.empty());
    }

    private boolean fullAndAcked(final List<Message> messagesInBucket) {
        return messagesInBucket.stream().allMatch(Message::isAcked) &&
               messagesInBucket.size() == queueDefinition.getBucketSize().get();
    }

    private boolean finalizedAndAllAcked(final List<Message> messagesInBucket, final BucketPointer invisBucketPointer) {
        return messagesInBucket.stream().allMatch(Message::isAcked) &&
               dataContext.getMessageRepository().finalizedExists(invisBucketPointer);
    }

    /**
     * in the active bucket, if there is a not acked, currently invisible
     * then if its ID is LESS than the current active pointer, move the pointer to that
     * otherwise pointer stays the same
     **/
    private boolean stopAtInvisMessageInBucket(final List<Message> messages, final InvisibilityMessagePointer pointer) {

        final Optional<Message> stoppingPoint = messages.stream()
                                                        .filter(m -> m.isNotAcked() &&
                                                                     m.isNotVisible(clock)).findFirst();

        if (stoppingPoint.isPresent()) {
            trySetNewInvisPointer(pointer, stoppingPoint.get().getIndex());

            logger.with(stoppingPoint.get()).info("Found invis message in current bucket");

            return true;
        }

        return false;
    }

    /**
     * If there is a message in the bucket that was never delivered,
     * dont move past it.
     *
     * @param messages
     * @param pointer
     * @return
     */
    private boolean stopAtNonDeliveredMessage(final List<Message> messages, final InvisibilityMessagePointer pointer) {
        final Optional<Message> nonDeliveredInBucket = messages.stream().filter(i -> i.getDeliveryCount() == 0).findFirst();

        if (nonDeliveredInBucket.isPresent()) {
            // move pointer to start of bucket since invis isn't set to anything yet
            // and this is going to be returned next
            trySetNewInvisPointer(pointer, nonDeliveredInBucket.get().getIndex());

            // don't move past a non delivered
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
    private Optional<Message> consumeRevivedMessageInBucket(
            final List<Message> messagesInBucket,
            final Duration invisiblity,
            InvisibilityMessagePointer currentPointer) {
        // any message in the bucket that is revived (delivery count > 0 and is visible non-acked)
        // OR a message that
        final Optional<Message> revivedMessage = messagesInBucket.stream()
                                                                 .filter(m -> m.isVisible(clock) && !m.isAcked())
                                                                 .findFirst();

        if (revivedMessage.isPresent()) {
            // stop here since we're scanning and we can't have anyone else behind us
            // since they are either invis, OR they are acked
            // attempt to consume the message
            final Optional<Message> tryConsumedMessage = dataContext.getMessageRepository()
                                                                    .consumeMessage(revivedMessage.get(), invisiblity);

            // if we were able to consume the message, try and move the invis pointer to this since its going to now be invis.
            // if someone else finds an earlier invis, it'll get moved to that
            // we can do this only because we don't already have an invis pointer
            if (tryConsumedMessage.isPresent()) {
                trySetNewInvisPointer(currentPointer, revivedMessage.get().getIndex());

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
    private Optional<InvisibilityMessagePointer> tryAdvancePointerToNextBucket(InvisibilityMessagePointer pointer) {
        final BucketPointer bucketPointer = pointer.toBucketPointer(queueDefinition.getBucketSize());

        if (bucketPointer.get().equals(currentReaderBucket.get())) {
            logger.with("reader-bucket", currentReaderBucket)
                  .with("current-invis-bucket", bucketPointer)
                  .warn("Attempting to move past reader bucket");

            return Optional.empty();
        }

        final MonotonicIndex monotonicIndex = bucketPointer.next().startOf(queueDefinition.getBucketSize());

        return Optional.of(trySetNewInvisPointer(pointer, InvisibilityMessagePointer.valueOf(monotonicIndex)));
    }

    private InvisibilityMessagePointer trySetNewInvisPointer(final InvisibilityMessagePointer currentInvis, MessagePointer potentialNextInvisPointer) {
        return dataContext.getPointerRepository().tryMoveInvisiblityPointerTo(currentInvis, InvisibilityMessagePointer.valueOf(potentialNextInvisPointer.get()));
    }
}
