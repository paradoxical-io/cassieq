package io.paradoxical.cassieq.unittests;

import categories.BuildVerification;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueAlreadyDeletingException;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;
import io.paradoxical.cassieq.workers.QueueDeleter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class QueueRepositoryTester extends DbTestBase {
    @Test
    public void queue_operations() throws Exception {
        final QueueName queueName = QueueName.valueOf("queue_operations");
        final TestQueueContext testContext = createTestQueueContext(queueName);

        assertThat(testContext.getQueueRepository().getQueueUnsafe(queueName).isPresent()).isEqualTo(true);
        assertThat(testContext.getQueueRepository().getActiveQueue(queueName).isPresent()).isEqualTo(true);

        assertThat(testContext.getQueueRepository().getQueueNames()).contains(queueName);
    }

    @Test
    public void queue_size_grows_and_shrinks() throws Exception {
        final QueueName queueName = QueueName.valueOf("queue_size_grows_and_shrinks");
        final TestQueueContext testContext = createTestQueueContext(queueName);

        int numMessages = 10;

        for (int i = 0; i < numMessages; i++) {
            testContext.putMessage(Integer.valueOf(i).toString());
        }

        final QueueRepository queueRepository = testContext.getQueueRepository();

        Optional<Long> queueSize = queueRepository.getQueueSize(testContext.getQueueDefinition());

        assertThat(queueSize.get()).isEqualTo(numMessages);

        int ackCount = 0;
        for (int i = numMessages - 1; i >= 0; i--) {
            ackCount++;

            testContext.readAndAckMessage(Integer.valueOf(i).toString());

            final Optional<Long> newQueueSize = queueRepository.getQueueSize(testContext.getQueueDefinition());

            assertThat(newQueueSize.get()).isEqualTo(numMessages - ackCount);

        }

        queueSize = queueRepository.getQueueSize(testContext.getQueueDefinition());

        assertThat(queueSize.get()).isEqualTo(0);
    }

    @Test
    public void cannot_create_same_queue_twice() throws Exception {
        final QueueName queueName = QueueName.valueOf("cannot_create_same_queue_twice");

        final TestQueueContext testContext = createTestQueueContext(queueName);

        final QueueRepository queueRepository = testContext.getQueueRepository();

        assertThat(queueRepository.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);
        assertThat(queueRepository.getActiveQueue(queueName).isPresent()).isEqualTo(true);

        assertThat(queueRepository.getQueueNames()).contains(queueName);

        assertThat(queueRepository.createQueue(testContext.getQueueDefinition())).isEmpty();
    }

    @Test
    public void queue_definition_once_in_status_cant_move_backwards() {

        final QueueName queueName = QueueName.valueOf("queue_definition_once_in_status_cant_move_backwards");


        final TestQueueContext testContext = createTestQueueContext(queueName);
        final QueueRepository queueRepository = testContext.getQueueRepository();

        final QueueDefinition queueDefinition = QueueDefinition.builder().accountName(testAccountName).queueName(queueName).build();

        queueRepository.createQueue(queueDefinition);

        assertThat(queueRepository.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);

        assertThat(queueRepository.getQueueUnsafe(queueName).get().getStatus()).isEqualTo(QueueStatus.Active);

        // no skipping a state
        assertThat(queueRepository.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Inactive)).isFalse();

        assertThat(queueRepository.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.PendingDelete)).isTrue();

        // ok to move to the same again
        assertThat(queueRepository.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Deleting)).isTrue();
        assertThat(queueRepository.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Deleting)).isTrue();

        assertThat(queueRepository.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Inactive)).isTrue();

        // can't go backwards
        assertThat(queueRepository.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Active)).isFalse();
    }

    @Test
    public void can_create_new_queue_while_old_queue_is_deleting() {

        final QueueName queueName = QueueName.valueOf("can_create_new_queue_while_old_queue_is_deleting");

        final TestQueueContext testContext = createTestQueueContext(queueName);
        final QueueRepository queueRepository = testContext.getQueueRepository();

        final QueueDefinition queueDefinition = QueueDefinition.builder().accountName(testAccountName).queueName(queueName).build();

        queueRepository.createQueue(queueDefinition);

        assertThat(queueRepository.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);

        queueRepository.tryMarkForDeletion(queueDefinition);

        // should be able to create a new definition here
        queueRepository.createQueue(queueDefinition);
    }

    @Test
    public void delete_queue() throws QueueAlreadyDeletingException {

        final QueueName queueName = QueueName.valueOf("delete_queue");

        final TestQueueContext testContext = createTestQueueContext(queueName);
        final QueueRepository queueRepository = testContext.getQueueRepository();

        final QueueDefinition queueDefinition = testContext.getQueueDefinition();

        queueRepository.createQueue(queueDefinition);

        assertThat(queueRepository.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);

        assertThat(queueRepository.getQueueNames()).contains(queueName);

        final QueueDeleter queueDeleter = testContext.createQueueDeleter();

        queueDeleter.delete(queueName);

        assertThat(queueRepository.getActiveQueue(queueName).isPresent())
                .isEqualTo(false)
                .withFailMessage("Deleted queue still shows up as active");

        assertThat(queueRepository.getQueueUnsafe(queueName).isPresent())
                .isEqualTo(false)
                .withFailMessage("Raw queue definition should not have existed. Queue definition was not removed");

        assertThat(queueRepository.getQueueNames()).doesNotContain(queueName);
    }

    @Test
    public void counters_work_after_complete_delete() throws Exception {

        final QueueName queueName = QueueName.valueOf("counters_work_after_complete_delete");

        final TestQueueContext testContext = createTestQueueContext(queueName);

        final QueueRepository queueRepository = testContext.getQueueRepository();

        final QueueDefinition queueDefinition = testContext.getQueueDefinition();

        assertThat(queueRepository.getQueueSize(queueDefinition)).isEmpty();

        assertThat(queueRepository.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);

        assertThat(queueRepository.getQueueNames()).contains(queueName);

        // attempt a delete

        final QueueDeleter queueDeleter = testContext.createQueueDeleter();

        queueDeleter.delete(queueName);

        assertThat(queueRepository.getActiveQueue(queueName).isPresent())
                .isEqualTo(false)
                .withFailMessage("Deleted queue still shows up as active");

        assertThat(queueRepository.getQueueUnsafe(queueName).isPresent())
                .isEqualTo(false)
                .withFailMessage("Raw queue definition should not have existed. Queue definition was not removed");

        assertThat(queueRepository.getQueueNames()).doesNotContain(queueName);

        // make sure that after a delete occurred we can get a valid counter result
        // it should have a new counter id
        final QueueDefinition recreatedDefinition = queueRepository.createQueue(queueDefinition).get();

        final TestQueueContext newContext = new TestQueueContext(recreatedDefinition, getDefaultInjector());

        newContext.putMessage("0");

        final Long afterRecreateSize = newContext.getQueueRepository().getQueueSize(recreatedDefinition).get();

        assertThat(afterRecreateSize).isEqualTo(1);
    }

    @Test
    public void only_one_active_ever() {

        final QueueName queueName = QueueName.valueOf("only_one_active_ever");

        final TestQueueContext testContext = createTestQueueContext(queueName);
        final QueueRepository queueRepository = testContext.getQueueRepository();

        final QueueDeleter queueDeleter = testContext.createQueueDeleter();

        final QueueDefinition queueDefinition = QueueDefinition.builder().accountName(testAccountName).queueName(queueName).build();

        IntStream.range(0, 1000)
                 .parallel()
                 .forEach(i -> {
                     queueRepository.createQueue(queueDefinition);

                     try {
                         queueDeleter.delete(queueName);
                     }
                     catch (QueueAlreadyDeletingException e) {
                         // ok
                     }
                 });


        queueRepository.createQueue(queueDefinition);

        final long count = queueRepository.getActiveQueues().stream().filter(i -> i.getQueueName().equals(queueName)).count();

        assertThat(count).isBetween(0L, 1L).withFailMessage("Too many active queues created");
    }
}
