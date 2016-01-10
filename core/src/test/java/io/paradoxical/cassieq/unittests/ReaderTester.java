package io.paradoxical.cassieq.unittests;

import categories.BuildVerification;
import com.google.inject.Injector;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MessageUpdateRequest;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.unittests.time.TestClock;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class ReaderTester extends TestBase {
    private final Injector defaultInjector;

    public ReaderTester() {
        this.defaultInjector = getDefaultInjector();
    }

    @Before
    public void setup() {
    }

    @Test
    public void invis_stops_non_finalized_bucket() throws Exception {
        final TestQueueContext testContext = setupTestContext("invis_stops_non_finalized_bucket", 3);

        testContext.putMessage(0, "1");
        testContext.putMessage(3, "2");

        testContext.ghostMessage();

        testContext.putMessage(0, "4 (new bucket)");
        testContext.putMessage(30, "5");
        testContext.putMessage(20, "6");

        testContext.readAndAckMessage("1");

        // 2 is invis, 3 is a ghost
        testContext.readAndAckMessage("4 (new bucket)");

        assertThat(testContext.readNextMessage(10)).isEmpty();

        getTestClock().tickSeconds(10L);

        // 2 is alive now
        final Message messageTwo = testContext.readNextMessage(1).get();

        // 2 is now invis, 5 and 6 are still blocked
        assertThat(testContext.readNextMessage(0)).isEmpty();

        // 2 is expired, should come back from invis
        getTestClock().tickSeconds(2L);

        testContext.readAndAckMessage("2");

        final InvisibilityMessagePointer currentInvisPointer = testContext.getContext().getPointerRepository().getCurrentInvisPointer();

        // it should point to message 2
        assertThat(currentInvisPointer.get()).isEqualTo(1);

        // repair worker processed and closed bucket 0 off
        testContext.finalize(0);

        // invis can now move on since all messages are ack'd in bucket 0
        // and there will be no more publishes (its been finalized)

        // however, message 5 is still invis
        assertThat(testContext.readNextMessage(10)).isEmpty();

        // but validate that the invis points to 5

        final InvisibilityMessagePointer nextInvisPointer = testContext.getContext().getPointerRepository().getCurrentInvisPointer();

        // it should point to message 5
        final Message messageFive = testContext.getContext().getMessageRepository().getMessage(nextInvisPointer);

        assertThat(messageFive.getBlob()).isEqualTo("5");
    }

    @Test
    public void initial_inivis_picked_up() throws Exception {
        final TestQueueContext testContext = setupTestContext("initial_inivis_picked_up", 3);

        testContext.putMessage(10, "1");
        testContext.putMessage(5, "2");
        testContext.putMessage(12, "3");
        testContext.putMessage(0, "4 (new bucket)");
        testContext.putMessage(0, "5");
        testContext.putMessage(1, "6");

        // reader skipped all the invis and tombstoned the
        // buckets
        testContext.readAndAckMessage("4 (new bucket");
        testContext.readAndAckMessage("5");

        assertThat(testContext.readNextMessage(10)).isEmpty();

        getTestClock().tickSeconds(10L);

        // 1 is alive now
        testContext.readAndAckMessage("1");

        // 2 should also be alive since it was blocked by 1
        testContext.readAndAckMessage("2");

        // 3 is bocked
        assertThat(testContext.readNextMessage(10)).isEmpty();

        getTestClock().tickSeconds(5L);

        testContext.readAndAckMessage("3");
        testContext.readAndAckMessage("6");
    }

    @Test
    public void delivery_count_increases_after_message_expires_and_is_redelivered() throws Exception {
        final TestQueueContext testContext = setupTestContext("delivery_count_increases_after_message_expires_and_is_redelivered");

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
        final TestQueueContext testContext = setupTestContext("reader_stops_on_deleted_queue");

        testContext.putMessage(0, "hi");

        getTestClock().tick();

        testContext.getQueueRepository().tryMarkForDeletion(testContext.getQueueDefinition());

        Optional<Message> message = testContext.getReader().nextMessage(Duration.standardSeconds(10));

        assertThat(message).isEmpty();
    }

    @Test
    public void test_ack_next_message() throws Exception {
        final TestQueueContext testContext = setupTestContext("test_ack_next_message");

        testContext.putMessage(0, "hi");

        getTestClock().tick();

        assertThat(testContext.readAndAckMessage("hi", 100L)).isTrue();

        getTestClock().tick();

        assertThat(testContext.readNextMessage(Duration.standardSeconds(1)).isPresent()).isFalse();
    }

    @Test
    public void test_acked_message_should_never_be_visible() throws Exception {
        final TestQueueContext testContext = setupTestContext("test_ack_next_message_should_never_be_visible");

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
        final TestQueueContext testContext = setupTestContext("invis_pointer_scan_finds_newly_alive_messages", 3);

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

        // inivs shorter is alive now but we can't getAccountRepository to it since
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
        final TestQueueContext testContext = setupTestContext("update_message_should_extend_invisiblity_time_and_be_ackable", 3);

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
        final TestQueueContext testContext = setupTestContext("update_message_should_extend_invisibility_time_and_still_expire", 3);

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
        final TestQueueContext testContext = setupTestContext("test_nack_via_update", 3);

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
        final TestQueueContext testContext = setupTestContext("test_monoton_skipped", bucketSize);
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

    private TestQueueContext setupTestContext(String queueName) {
        return setupTestContext(queueName, 20);
    }

    private TestQueueContext setupTestContext(String queueName, int bucketSize) {
        final QueueName queue = QueueName.valueOf(queueName);
        final QueueDefinition queueDefinition = QueueDefinition.builder()
                                                               .queueName(queue)
                                                               .bucketSize(BucketSize.valueOf(bucketSize))
                                                               .build();
        createQueue(queueDefinition);

        return new TestQueueContext(queueDefinition, getDefaultInjector());
    }
}
