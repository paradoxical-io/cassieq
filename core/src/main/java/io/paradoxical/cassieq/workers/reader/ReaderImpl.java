package io.paradoxical.cassieq.workers.reader;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.QueueDataContext;
import io.paradoxical.cassieq.model.BucketPointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.PopReceipt;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.time.Clock;
import io.paradoxical.cassieq.workers.DefaultMessageConsumer;
import io.paradoxical.cassieq.workers.MessageConsumer;
import lombok.Cleanup;
import org.joda.time.Duration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static com.godaddy.logging.LoggerFactory.getLogger;

public class ReaderImpl implements Reader {
    private final QueueRepository queueRepository;
    private Logger logger = getLogger(ReaderImpl.class);

    private final QueueDataContext dataContext;
    private final DataContextFactory dataContextFactory;
    private static final Random random = new Random();
    private final Clock clock;
    private final MetricRegistry metricRegistry;
    private final InvisStrategy.Factory invisStrategyFactory;
    private final QueueDefinition queueDefinition;
    private final MessageConsumer messageConsumer;
    private final Supplier<Timer.Context> timerSupplier;

    @Inject
    public ReaderImpl(
            DataContextFactory dataContextFactory,
            Clock clock,
            MetricRegistry metricRegistry,
            DefaultMessageConsumer.Factory messageConsumerFactory,
            InvisStrategy.Factory invisStrategyFactory,
            @Assisted AccountName accountName,
            @Assisted QueueDefinition queueDefinition) {
        this.dataContextFactory = dataContextFactory;
        this.clock = clock;
        this.metricRegistry = metricRegistry;
        this.invisStrategyFactory = invisStrategyFactory;
        messageConsumer = messageConsumerFactory.forQueue(queueDefinition);
        this.queueDefinition = queueDefinition;

        dataContext = dataContextFactory.forQueue(queueDefinition);
        queueRepository = dataContextFactory.forAccount(accountName);

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

        final Optional<Message> nowVisibleMessage = getNewlyVisible(invisibility);

        if (nowVisibleMessage.isPresent()) {
            return nowVisibleMessage;
        }

        return getNext(invisibility);
    }

    /**
     * Gets the next visible message
     *
     * @param invisibility
     * @return
     */
    private Optional<Message> getNext(Duration invisibility) {
        final Optional<Message> nextMessage = getAndMark(getReaderCurrentBucket(), invisibility);

        if (nextMessage.isPresent()) {
            logger.with(nextMessage.get()).info("Got message");
        }

        return nextMessage;
    }

    /**
     * Asks the invis strategy for the next visible message
     *
     * @param invisibility
     * @return
     */
    private Optional<Message> getNewlyVisible(Duration invisibility) {
        final Optional<Message> nowVisibleMessage = invisStrategyFactory.forQueue(queueDefinition)
                                                                        .findNextVisibleMessage(invisibility);

        if (nowVisibleMessage.isPresent()) {
            logger.with(nowVisibleMessage.get()).info("Got newly visible message");

            final ConsumableMessage message = new ConsumableMessage(nowVisibleMessage.get(), invisibility, Source.InvisStrategy);

            final Optional<Message> consumedMessage = tryConsume(message);

            if (consumedMessage.isPresent()) {
                metricRegistry.counter(name("reader", "revived", "messages")).inc();

                return consumedMessage;
            }
        }

        return Optional.empty();
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

            final Optional<Message> foundMessage = findRandom(allMessages.stream().filter(m -> m.isNotAcked() && m.isVisible(clock)).collect(Collectors.toList()));

            if (!foundMessage.isPresent()) {
                return Optional.empty();
            }

            final ConsumableMessage consumableMessage = new ConsumableMessage(foundMessage.get(), invisiblity, Source.Reader);

            Optional<Message> consumedMessage = tryConsume(consumableMessage);

            if (consumedMessage.isPresent()) {
                return consumedMessage;
            }

            // loop again
        }
    }

    private Optional<Message> tryConsume(ConsumableMessage message) {
        final Optional<Message> consumedMessage = messageConsumer.tryConsume(message);

        if (!consumedMessage.isPresent()) {
            // someone else did it, fuck it, try again for the next visibleMessage
            logger.with(message).trace("Someone else consumed the visibleMessage!");

            return Optional.empty();
        }

        return consumedMessage;
    }

    private Optional<Message> findRandom(final List<Message> availableMessages) {
        final int size = availableMessages.size();

        if (size == 0) {
            return Optional.empty();
        }

        if (queueDefinition.isStrictFifo()) {
            Optional.of(availableMessages.get(0));
        }

        final int i = random.nextInt(size);

        return Optional.of(availableMessages.get(i));
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
