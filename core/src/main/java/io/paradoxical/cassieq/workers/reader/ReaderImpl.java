package io.paradoxical.cassieq.workers.reader;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.InvisLocaterFactory;
import io.paradoxical.cassieq.model.BucketPointer;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.PopReceipt;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.time.Clock;
import lombok.Cleanup;
import org.joda.time.Duration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;
import static com.godaddy.logging.LoggerFactory.getLogger;

public class ReaderImpl implements Reader {
    private Logger logger = getLogger(ReaderImpl.class);

    private final DataContext dataContext;
    private final QueueRepository queueRepository;
    private final Clock clock;
    private final MetricRegistry metricRegistry;
    private final InvisLocaterFactory invisLocaterFactory;
    private final QueueDefinition queueDefinition;
    private final Supplier<Timer.Context> timerSupplier;

    @Inject
    public ReaderImpl(
            DataContextFactory dataContextFactory,
            QueueRepository queueRepository,
            Clock clock,
            MetricRegistry metricRegistry,
            InvisLocaterFactory invisLocaterFactory,
            @Assisted QueueDefinition queueDefinition) {
        this.queueRepository = queueRepository;
        this.clock = clock;
        this.metricRegistry = metricRegistry;
        this.invisLocaterFactory = invisLocaterFactory;
        this.queueDefinition = queueDefinition;

        dataContext = dataContextFactory.forQueue(queueDefinition);

        logger = logger.with("q", queueDefinition.getQueueName()).with("version", queueDefinition.getVersion());

        timerSupplier = () -> metricRegistry.timer(name("reader", "queue", queueDefinition.getQueueName().get(), "v" + queueDefinition.getVersion()))
                                            .time();
    }

    @Override
    public Optional<Message> nextMessage(Duration invisibility) {
        if (!isActive()) {
            return Optional.empty();
        }

        @SuppressWarnings("unused")
        @Cleanup("stop")
        final Timer.Context readerTimer = timerSupplier.get();

        final Optional<Message> nowVisibleMessage = invisLocaterFactory.forQueue(queueDefinition)
                                                                       .tryConsumeNextVisibleMessage(getCurrentInvisPointer(), invisibility);

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
            logger.with(bucket).info("Tombstoned reader");
        }
        else {
            logger.with(bucket).info("Reader bucket was already tombstoned");
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
}
