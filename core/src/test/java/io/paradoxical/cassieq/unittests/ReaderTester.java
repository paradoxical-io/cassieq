package io.paradoxical.cassieq.unittests;

import com.google.inject.Injector;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MessageUpdateRequest;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.PopReceipt;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.unittests.time.TestClock;
import io.paradoxical.cassieq.workers.reader.Reader;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
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

    @Data
    class ReaderQueueContext {

        @NonNull
        private final QueueName queueName;

        @NonNull
        private final QueueDefinition queueDefinition;

        @NonNull
        private final Reader reader;

        @Getter
        private DataContext context;

        public ReaderQueueContext(QueueDefinition queueDefinition, Reader reader) {
            this.reader = reader;
            this.queueDefinition = queueDefinition;
            this.queueName = queueDefinition.getQueueName();

            final DataContextFactory factory = defaultInjector.getInstance(DataContextFactory.class);

            context = factory.forQueue(queueDefinition);
        }

        public Optional<Message> readNextMessage(Duration invisiblity) {
            return reader.nextMessage(invisiblity);
        }

        public Optional<Message> readNextMessage(int invisiblitySeconds) {
            return reader.nextMessage(Duration.standardSeconds(invisiblitySeconds));
        }

        private void putMessage(String blob) throws Exception {
            putMessage(0, blob);
        }

        private void putMessage(int seconds, String blob) throws Exception {

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

            final PopReceipt popReceipt = PopReceipt.from(message.get());

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

        getQueueRepository().tryMarkForDeletion(testContext.queueDefinition);

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
    public void invis_pointer_scan_finds_newly_alive_messages() throws Exception {
        // make the queue bucket size smaller so that the reader moves past it
        final ReaderQueueContext testContext = setupTestContext("invis_pointer_scan_finds_newly_alive_messages", 3);

        testContext.putMessage(0, "ok");
        testContext.putMessage(0, "invis blocker");
        testContext.putMessage(0, "invis shorter");
        testContext.putMessage(0, "ok2");
        testContext.putMessage(0, "ok3");
        testContext.putMessage(0, "ok4");

        final TestClock testClock = getTestClock();

        testContext.readAndAckMessage("message 1", 1L);

        testClock.tickSeconds(1L);

        final Message invisBlocker = testContext.getReader().nextMessage(Duration.standardSeconds(5)).get();

        final Message invisShorter = testContext.getReader().nextMessage(Duration.standardSeconds(1)).get();

        // the reader bucket is advanced here to bucket 1
        testContext.readAndAckMessage("ok2", 10L);

        // inivs shorter is alive now but we can't get to it since
        // its held up by invis blocker
        testClock.tickSeconds(2L);

        testContext.readAndAckMessage("ok3", 0L);

        // invisBlocker is alive
        testClock.tickSeconds(10L);

        testContext.readAndAckMessage("invis blocker", 0L);
        testContext.readAndAckMessage("invis shorter", 0L);
        testContext.readAndAckMessage("ok4", 0L);
    }

    @Test
    public void update_message_should_extend_invisiblity_time_and_be_ackable() throws Exception {
        final ReaderQueueContext testContext = setupTestContext("update_message_should_extend_invisiblity_time_and_be_ackable", 3);

        testContext.putMessage("init");

        final Message message = testContext.readNextMessage(10).get();

        final MessageUpdateRequest messageUpdateRequest =
                new MessageUpdateRequest(Duration.standardSeconds(20),
                                         message.getTag(),
                                         message.getVersion(),
                                         message.getIndex(),
                                         "init2");

        final Optional<Message> updatedMessage = testContext.getContext().getMessageRepository().updateMessage(messageUpdateRequest);

        assertThat(updatedMessage).isPresent();

        // make sure that updating didn't actually change any reader logic
        assertThat(testContext.readNextMessage(10)).isEmpty();

        // the initial request _would_ have expired here
        getTestClock().tickSeconds(12L);

        // make sure that we haven't actually expired yet
        assertThat(testContext.readNextMessage(10)).isEmpty();

        // ack the extended message
        assertThat(testContext.getReader().ackMessage(updatedMessage.get().getPopReceipt())).isTrue();

        // make sure there isn't anything left
        final Optional<Message> newlyExpiredMessage = testContext.readNextMessage(10);

        assertThat(newlyExpiredMessage).isEmpty();
    }

    @Test
    public void update_message_should_extend_invisibility_time_and_still_expire() throws Exception {
        final ReaderQueueContext testContext = setupTestContext("update_message_should_extend_invisibility_time_and_still_expire", 3);

        testContext.putMessage("init");

        final Message message = testContext.readNextMessage(10).get();

        final MessageUpdateRequest messageUpdateRequest =
                new MessageUpdateRequest(Duration.standardSeconds(20),
                                         message.getTag(),
                                         message.getVersion(),
                                         message.getIndex(),
                                         "init2");

        final Optional<Message> updatedMessage = testContext.getContext().getMessageRepository().updateMessage(messageUpdateRequest);


        assertThat(updatedMessage).isPresent();

        // make sure that updating didn't actually change any reader logic
        assertThat(testContext.readNextMessage(10)).isEmpty();

        // the initial request _would_ have expired here
        getTestClock().tickSeconds(12L);

        // make sure that we haven't actually expired yet
        assertThat(testContext.readNextMessage(10)).isEmpty();

        // actually expire
        getTestClock().tickSeconds(10L);

        final Optional<Message> newlyExpiredMessage = testContext.readNextMessage(10);

        assertThat(newlyExpiredMessage).isPresent();

        assertThat(newlyExpiredMessage.get().getTag()).isEqualTo(message.getTag());
        assertThat(newlyExpiredMessage.get().getVersion()).isGreaterThan(message.getVersion());
        assertThat(newlyExpiredMessage.get().getBlob()).isEqualTo(messageUpdateRequest.getNewBlob());
    }

    @Test
    public void test_nack_via_update() throws Exception {
        final ReaderQueueContext testContext = setupTestContext("test_nack_via_update", 3);

        testContext.putMessage("init");

        final Message originalMessage = testContext.readNextMessage(10).get();

        final MessageUpdateRequest messageUpdateRequest =
                new MessageUpdateRequest(Duration.standardSeconds(0),
                                         originalMessage.getTag(),
                                         originalMessage.getVersion(),
                                         originalMessage.getIndex(),
                                         "init2");

        final Optional<Message> updatedMessage = testContext.getContext().getMessageRepository().updateMessage(messageUpdateRequest);


        assertThat(updatedMessage).isPresent();

        // we nacked the original, so see if it comes back
        final Optional<Message> previouslyNackedMessage = testContext.readNextMessage(10);

        assertThat(previouslyNackedMessage).isPresent();
        assertThat(previouslyNackedMessage.get().getTag()).isEqualTo(originalMessage.getTag());
        assertThat(previouslyNackedMessage.get().getVersion()).isGreaterThan(originalMessage.getVersion());
        assertThat(previouslyNackedMessage.get().getBlob()).isEqualTo(messageUpdateRequest.getNewBlob());
        assertThat(previouslyNackedMessage.get().getDeliveryCount()).isEqualTo(originalMessage.getDeliveryCount() + 1);

        // we nacked the original pop reciept so we shouldn't be able to ack it anymore
        assertThat(testContext.getReader().ackMessage(originalMessage.getPopReceipt())).isFalse();
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

        return new ReaderQueueContext(queueDefinition, reader);
    }

    private QueueRepository getQueueRepository() {
        return getDefaultInjector().getInstance(QueueRepository.class);
    }
}
