package io.paradoxical.cassieq.unittests;

import categories.BuildVerification;
import com.google.inject.Injector;
import io.paradoxical.cassieq.dataAccess.DeletionJob;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageDeleterJobProcessor;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.MessageDeleterJobProcessorFactory;
import io.paradoxical.cassieq.factories.QueueDataContext;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class MessageRepositoryTester extends DbTestBase {
    @Test
    public void put_message_should_succeed() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final DataContextFactory factory = defaultInjector.getInstance(DataContextFactory.class);

        final QueueName queueName = QueueName.valueOf("put_message_should_succeed");

        final QueueDefinition queueDefinition = setupQueue(queueName);

        final QueueDataContext context = factory.forQueue(queueDefinition);

        final MonotonicIndex monoton = context.getMonotonicRepository().nextMonotonic();

        context.getMessageRepository().putMessage(
                Message.builder()
                       .blob("hi")
                       .index(monoton)
                       .build(), Duration.standardSeconds(30));

        final List<Message> messages = context.getMessageRepository().getMessages(() -> 0L);

        assertThat(messages.size()).isEqualTo(1);
    }

    @Test
    public void dead_letter_messages_should_not_reappear() throws Exception {
        final QueueName queueName = QueueName.valueOf("dead_letter_messages_should_not_reappear");

        final TestQueueContext context = setupTestContext(QueueDefinition.builder().accountName(testAccountName).maxDeliveryCount(3).queueName(queueName).build());

        context.putMessage("1");

        readTillMessagePoisoned(context);

        assertThat(context.readNextMessage(1).isPresent()).isFalse();
    }

    @Test
    public void dead_letter_messages_should_be_republished_to_dlq() throws Exception {
        final QueueName queueName = QueueName.valueOf("dead_letter_messages_should_be_republished_to_dlq");

        final QueueDefinition dlqDefinition = QueueDefinition.builder().accountName(testAccountName).queueName(QueueName.valueOf(queueName.get() + "_dlq")).build();

        final TestQueueContext dlqContext = setupTestContext(dlqDefinition);

        final QueueDefinition queueDefinition = QueueDefinition.builder()
                                                               .accountName(testAccountName)
                                                               .maxDeliveryCount(3)
                                                               .queueName(queueName)
                                                               .dlqName(Optional.of(dlqDefinition.getQueueName()))
                                                               .build();

        final TestQueueContext context = setupTestContext(queueDefinition);

        context.putMessage("1");

        readTillMessagePoisoned(context);

        assertThat(context.readNextMessage(1).isPresent()).isFalse();

        assertThat(dlqContext.readNextMessage(1)).isPresent();
    }

    private void readTillMessagePoisoned(final TestQueueContext context) {
        long workerProcessTime = 1L;

        for (int i = 0; i < context.getQueueDefinition().getMaxDeliveryCount(); i++) {
            final Optional<Message> poisonMessage = context.readNextMessage((int) workerProcessTime);

            assertThat(poisonMessage.isPresent()).isTrue();

            assertThat(poisonMessage.get().getBlob()).isEqualTo("1");

            // pretend the worker is taking too long, so this message is somehow poisonous
            // it should come back `max delivery count` number of times
            getTestClock().tickSeconds(2 * workerProcessTime);
        }
    }

    @Test
    public void ack_message_should_succeed() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final DataContextFactory factory = defaultInjector.getInstance(DataContextFactory.class);
        final QueueName queueName = QueueName.valueOf("ack_message_should_succeed");
        final QueueDefinition queueDefinition = setupQueue(queueName);

        final QueueDataContext context = factory.forQueue(queueDefinition);

        final MonotonicIndex monoton = context.getMonotonicRepository().nextMonotonic();

        context.getMessageRepository().putMessage(
                Message.builder()
                       .blob("hi")
                       .index(monoton)
                       .build(), Duration.standardSeconds(30));


        final Message message = context.getMessageRepository().getMessage(monoton);

        assertThat(message.isAcked()).isFalse();

        final boolean ackSucceeded = context.getMessageRepository().ackMessage(message);
        assertThat(ackSucceeded).isTrue();

        final Message ackedMessage = context.getMessageRepository()
                                            .getMessage(message.getIndex());

        assertThat(ackedMessage.isAcked()).isTrue();
    }

    @Test
    public void ack_message_after_version_changed_should_fail() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final DataContextFactory factory = defaultInjector.getInstance(DataContextFactory.class);

        final QueueName queueName = QueueName.valueOf("ack_message_after_version_changed_should_fail");

        final QueueDefinition queueDefinition = setupQueue(queueName);

        final QueueDataContext context = factory.forQueue(queueDefinition);

        final MonotonicIndex monoton = context.getMonotonicRepository().nextMonotonic();

        context.getMessageRepository().putMessage(
                Message.builder()
                       .blob("hi")
                       .index(monoton)
                       .build(), Duration.millis(30));

        final Message message = context.getMessageRepository().getMessage(monoton);

        assertThat(message.isAcked()).isFalse();

        final Optional<Message> consumedMessage = context.getMessageRepository().rawConsumeMessage(message, Duration.standardDays(30));

        assertThat(consumedMessage.isPresent()).isTrue();
        assertThat(message.getVersion()).isNotEqualTo(consumedMessage.get().getVersion());

        final boolean ackSucceeded = context.getMessageRepository().ackMessage(message);
        assertThat(ackSucceeded).isFalse();

        final Message ackedMessage = context.getMessageRepository()
                                            .getMessage(message.getIndex());

        assertThat(ackedMessage.isAcked()).isFalse();
    }

    @Test
    public void an_added_tombstone_should_exist() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final DataContextFactory factory = defaultInjector.getInstance(DataContextFactory.class);
        final QueueName queueName = QueueName.valueOf("an_added_tombstone_should_exist");

        final QueueDefinition queueDefinition = setupQueue(queueName);

        final QueueDataContext context = factory.forQueue(queueDefinition);

        final MonotonicIndex monoton = context.getMonotonicRepository().nextMonotonic();

        context.getMessageRepository().tombstone(monoton.toBucketPointer(BucketSize.valueOf(1)));

        final Optional<DateTime> tombstoneExists = context.getMessageRepository().tombstoneExists(monoton.toBucketPointer(BucketSize.valueOf(1)));

        assertThat(tombstoneExists.isPresent()).isTrue();
    }

    @Test
    public void deleting_a_queue_deletes_all_messages() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final DataContextFactory factory = defaultInjector.getInstance(DataContextFactory.class);

        final QueueName queueName = QueueName.valueOf("deleting_a_queue_deletes_all_messages");

        final QueueDefinition queueDefinition = setupQueue(queueName);

        final QueueDataContext context = factory.forQueue(queueDefinition);

        final MonotonicIndex monoton = context.getMonotonicRepository().nextMonotonic();

        context.getMessageRepository().putMessage(
                Message.builder()
                       .blob("hi")
                       .index(monoton)
                       .build(), Duration.standardSeconds(30));

        final List<Message> messages = context.getMessageRepository().getMessages(() -> 0L);

        assertThat(messages.size()).isEqualTo(1).withFailMessage("Found incorrect number of messages in queue");

        final DeletionJob deletionJob = new DeletionJob(queueDefinition);

        final MessageDeleterJobProcessor messageDeletorJobProcessor =
                defaultInjector.getInstance(MessageDeleterJobProcessorFactory.class).createDeletionProcessor(deletionJob);

        messageDeletorJobProcessor.start();

        assertThat(context.getMessageRepository().getMessages(() -> 0L).size()).isEqualTo(0).withFailMessage("Values still existed in queue");
    }

    @Test
    public void deleting_a_bucket_removes_its_messages() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final DataContextFactory factory = defaultInjector.getInstance(DataContextFactory.class);

        final QueueName queueName = QueueName.valueOf("deleting_a_bucket_removes_its_messages");

        final QueueDefinition queueDefinition = setupQueue(queueName);

        final QueueDataContext context = factory.forQueue(queueDefinition);

        final MonotonicIndex monoton = context.getMonotonicRepository().nextMonotonic();

        context.getMessageRepository().putMessage(
                Message.builder()
                       .blob("hi")
                       .index(monoton)
                       .build(), Duration.standardSeconds(30));

        final List<Message> messages = context.getMessageRepository().getMessages(() -> 0L);

        assertThat(messages.size()).isEqualTo(1).withFailMessage("Found incorrect number of messages in queue");

        context.getMessageRepository().deleteAllMessages(RepairBucketPointer.valueOf(0));

        assertThat(context.getMessageRepository().getMessages(() -> 0L).size()).isEqualTo(0).withFailMessage("Values still existed in queue");
    }
}

