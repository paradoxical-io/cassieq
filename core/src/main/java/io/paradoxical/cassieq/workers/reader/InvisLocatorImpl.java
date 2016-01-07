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
import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;
import static com.godaddy.logging.LoggerFactory.getLogger;


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
    private Logger logger = getLogger(InvisLocatorImpl.class);

    private final Clock clock;
    private final MetricRegistry metricRegistry;
    private final QueueDefinition queueDefinition;
    private final DataContext dataContext;
    private final ReaderBucketPointer currentReaderBucket;


    @Inject
    public InvisLocatorImpl(
            DataContextFactory dataContextFactory,
            Clock clock,
            MetricRegistry metricRegistry,
            @Assisted QueueDefinition queueDefinition) {
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
            final InvisMessagePointerProcessResult invisMessagePointerProcessResult = processCurrentInvisPointer(activePointer, invisiblity);

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
                            if (nextBucketStartPointer.isPresent()) {
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

    private InvisMessagePointerProcessResult processCurrentInvisPointer(InvisibilityMessagePointer pointer, Duration invisibility) {
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

        // no messages
        if (messagesInBucket.isEmpty()) {
            // if we're at the last bucket anyways, can't move pointer since nothing to move to
            if (invisBucketPointer.get() >= currentReaderBucketPointer.get()) {
                return InvisBucketProcessResult.of(Optional.empty());
            }

            // a finalizer exists so this bucket is done but the messages are deleted
            // we can be safe to move on, since a delete can occur only if all were ack'd and its finalized
            if(dataContext.getMessageRepository().finalizedExists(invisBucketPointer)) {
                return InvisBucketProcessResult.nextBucket();
            }

            // else we're on a fresh bucket, just stay where we are
            return InvisBucketProcessResult.of(Optional.empty());
        }

        // if the reader has moved past the current invis bucket, check
        // if there are messages that are alive and were skipped
        if (currentReaderBucketPointer.get() > invisBucketPointer.get()) {
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

        // if everything is ack'd but there are messages
        // (either the queue hasn't run the finalizer, or its a test
        // or the user elected to not delete messages on finalization)
        // then move to the next bucket since there are no invis here
        if (allAcked(messagesInBucket, invisBucketPointer)) {
            return InvisBucketProcessResult.nextBucket();
        }

        // not all messages are acked and the bucket isn't finalized yet
        // and no invis or revived messages were found
        return InvisBucketProcessResult.of(Optional.empty());
    }

    private boolean allAcked(final List<Message> messagesInBucket, final BucketPointer invisBucketPointer) {
        final boolean bucketIsFullAndAcked = messagesInBucket.stream().allMatch(Message::isAcked) &&
                                             messagesInBucket.size() == queueDefinition.getBucketSize().get();


        final Supplier<Boolean> bucketisFinalizedAndFull = () ->
                messagesInBucket.stream().allMatch(Message::isAcked) &&
                dataContext.getMessageRepository().finalizedExists(invisBucketPointer);

        return bucketIsFullAndAcked || bucketisFinalizedAndFull.get();
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
