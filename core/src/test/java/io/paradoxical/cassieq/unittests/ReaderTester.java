package io.paradoxical.cassieq.unittests;

import com.google.inject.Injector;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.PopReceipt;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;
import io.paradoxical.cassieq.unittests.time.TestClock;
import io.paradoxical.cassieq.workers.reader.Reader;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ReaderTester extends TestBase {
    private final Injector defaultInjector;

    public ReaderTester() {
        this.defaultInjector = getDefaultInjector();
    }

    @Value class ReaderQueueContext {

        @NonNull
        QueueName queueName;

        @NonNull
        QueueDefinition queueDefinition;

        @NonNull
        Reader reader;

        public Optional<Message> readNextMessage(Duration invisiblity) {
            return reader.nextMessage(invisiblity);
        }

        private void putMessage(int seconds, String blob) throws Exception {
            final DataContextFactory factory = defaultInjector.getInstance(DataContextFactory.class);
            final DataContext context = factory.forQueue(getQueueDefinition());

            final MonotonicIndex monoton = context.getMonotonicRepository().nextMonotonic();

            context.getMessageRepository().putMessage(
                    Message.builder()
                           .blob(blob)
                           .index(monoton)
                           .build(), Duration.standardSeconds(seconds));
        }

        private boolean readAndAckMessage(String blob, Long invisDuration) {
            Optional<Message> message = getReader().nextMessage(Duration.standardSeconds(invisDuration));

            assertThat(message.get().getBlob().equals(blob));

            final PopReceipt popReceipt = PopReceipt.from(message.get(), queueDefinition.getId());

            return reader.ackMessage(popReceipt);
        }
    }

    @Before
    public void setup() {
    }

    @Test
    public void delivery_count_increases_after_message_expires_and_is_redelivered() throws Exception {
        final ReaderQueueContext testContext = setupTestContext("delivery_count_increases_after_message_expires_and_is_redelivered");

        testContext.putMessage(0, "hi");

        getTestClock().tick();

        Optional<Message> message = testContext.getReader().nextMessage(Duration.standardSeconds(4));

        assertThat(message.isPresent()).isTrue();

        assertThat(message.get().getDeliveryCount()).isEqualTo(0);

        getTestClock().tickSeconds(5L);

        message = testContext.getReader().nextMessage(Duration.standardSeconds(4));

        assertThat(message.isPresent()).isTrue();

        assertThat(message.get().getDeliveryCount()).isEqualTo(1);
    }

    @Test
    public void reader_stops_on_deleted_queue() throws Exception {
        final ReaderQueueContext testContext = setupTestContext("reader_stops_on_deleted_queue");

        testContext.putMessage(0, "hi");

        getTestClock().tick();

        getQueueRepository().markForDeletion(testContext.queueDefinition);

        Optional<Message> message = testContext.getReader().nextMessage(Duration.standardSeconds(10));

        assertThat(message).isEmpty();
    }

    @Test
    public void test_ack_next_message() throws Exception {
        final ReaderQueueContext testContext = setupTestContext("test_ack_next_message");

        testContext.putMessage(0, "hi");

        getTestClock().tick();

        assertThat(testContext.readAndAckMessage("hi", 100L)).isTrue();

        getTestClock().tick();

        assertThat(testContext.readNextMessage(Duration.standardSeconds(1)).isPresent()).isFalse();
    }

    @Test
    public void test_acked_message_should_never_be_visible() throws Exception {
        final ReaderQueueContext testContext = setupTestContext("test_ack_next_message_should_never_be_visible");

        testContext.putMessage(0, "ghost");

        final TestClock testClock = getTestClock();

        testClock.tick();

        assertThat(testContext.readAndAckMessage("ghost", 1L)).isTrue();


        for (int i = 0; i < 10; i++) {
            testClock.tick();

            final Optional<Message> message = testContext.readNextMessage(Duration.millis(300));

            assertThat(message.isPresent()).isFalse();
        }
    }

    @Test
    public void test_monoton_skipped() throws Exception {
        final int bucketSize = 5;
        final ReaderQueueContext testContext = setupTestContext("test_monoton_skipped", bucketSize);
        final TestClock testClock = getTestClock();

        for (int i = 0; i < bucketSize - 1; i++) {
            testContext.putMessage(0, "foo");

            testClock.tick();

            assertThat(testContext.readAndAckMessage("foo", 100L)).isTrue();
        }

        //last monoton of the bucket is grabbed and will be skipped over.
        defaultInjector.getInstance(DataContextFactory.class)
                       .forQueue(testContext.getQueueDefinition())
                       .getMonotonicRepository()
                       .nextMonotonic();

        //Put message in new bucket, verify that message can be read after monoton was skipped and new bucket contains message.
        testContext.putMessage(0, "bar");

        testClock.tick();

        assertThat(testContext.readAndAckMessage("bar", 100L)).isTrue();
    }

    private ReaderQueueContext setupTestContext(String queueName) {
        return setupTestContext(queueName, 20);
    }

    private ReaderQueueContext setupTestContext(String queueName, int bucketSize) {
        final QueueName queue = QueueName.valueOf(queueName);
        final QueueDefinition queueDefinition = QueueDefinition.builder()
                                                               .queueName(queue)
                                                               .bucketSize(BucketSize.valueOf(bucketSize))
                                                               .build();
        createQueue(queueDefinition);

        final ReaderFactory readerFactory = defaultInjector.getInstance(ReaderFactory.class);

        final Reader reader = readerFactory.forQueue(queueDefinition);

        return new ReaderQueueContext(queue, queueDefinition, reader);
    }

    private QueueRepository getQueueRepository() {
        return getDefaultInjector().getInstance(QueueRepository.class);
    }
}
