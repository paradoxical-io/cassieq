package io.paradoxical.cassieq.unittests;

import categories.BuildVerification;
import com.google.inject.Injector;
import io.paradoxical.cassieq.dataAccess.QueueRepositoryImpl;
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
public class QueueRepositoryTester extends TestBase {
    @Test
    public void queue_operations() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("queue_operations");

        assertThat(repo.getQueueUnsafe(queueName).isPresent()).isEqualTo(false);

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);
    }

    @Test
    public void queue_size_grows_and_shrinks() throws Exception {
        final TestQueueContext testContext = new TestQueueContext(QueueName.valueOf("queue_size_grows_and_shrinks"), getDefaultInjector());

        int numMessages = 1000;

        for (int i = 0; i < numMessages; i++) {
            testContext.putMessage(Integer.valueOf(i).toString());
        }

        Optional<Long> queueSize = testContext.getQueueRepository().getQueueSize(testContext.getQueueDefinition());

        assertThat(queueSize.get()).isEqualTo(numMessages);

        int ackCount = 0;
        for (int i = numMessages - 1; i >= 0; i--) {
            ackCount++;

            testContext.readAndAckMessage(Integer.valueOf(i).toString());

            final Optional<Long> newQueueSize = testContext.getQueueRepository().getQueueSize(testContext.getQueueDefinition());

            assertThat(newQueueSize.get()).isEqualTo(numMessages - ackCount);

        }

        queueSize = testContext.getQueueRepository().getQueueSize(testContext.getQueueDefinition());

        assertThat(queueSize.get()).isEqualTo(0);
    }

    @Test
    public void cannot_create_same_queue_twice() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("cannot_create_same_queue_twice");

        assertThat(repo.getQueueUnsafe(queueName).isPresent()).isEqualTo(false);

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);

        assertThat(repo.createQueue(queueDefinition)).isEmpty();
    }

    @Test
    public void queue_definition_once_in_status_cant_move_backwards() {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepositoryImpl repo = (QueueRepositoryImpl) defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("queue_definition_once_in_status_cant_move_backwards");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);

        assertThat(repo.getQueueUnsafe(queueName).get().getStatus()).isEqualTo(QueueStatus.Active);

        // no skipping a state
        assertThat(repo.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Inactive)).isFalse();

        assertThat(repo.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.PendingDelete)).isTrue();

        // ok to move to the same again
        assertThat(repo.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Deleting)).isTrue();
        assertThat(repo.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Deleting)).isTrue();

        assertThat(repo.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Inactive)).isTrue();

        // can't go backwards
        assertThat(repo.tryAdvanceQueueStatus(queueDefinition.getQueueName(), QueueStatus.Active)).isFalse();
    }

    @Test
    public void can_create_new_queue_while_old_queue_is_deleting() {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("can_create_new_queue_while_old_queue_is_deleting");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);

        repo.tryMarkForDeletion(queueDefinition);

        // should be able to create a new definition here
        repo.createQueue(queueDefinition);
    }

    @Test
    public void delete_queue() throws QueueAlreadyDeletingException {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueDeleter queueDeleter = defaultInjector.getInstance(QueueDeleter.class);

        final QueueName queueName = QueueName.valueOf("delete_queue");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueueUnsafe(queueName).isPresent()).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);

        queueDeleter.delete(queueName);

        assertThat(repo.getActiveQueue(queueName).isPresent())
                .isEqualTo(false)
                .withFailMessage("Deleted queue still shows up as active");

        assertThat(repo.getQueueUnsafe(queueName).isPresent())
                .isEqualTo(false)
                .withFailMessage("Raw queue definition still exists even though deleter should have removed it");

        assertThat(repo.getQueueNames()).doesNotContain(queueName);
    }

    @Test
    public void only_one_active_ever() {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueDeleter queueDeleter = defaultInjector.getInstance(QueueDeleter.class);

        final QueueName queueName = QueueName.valueOf("only_one_active_ever");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        IntStream.range(0, 1000)
                 .parallel()
                 .forEach(i -> {
                     repo.createQueue(queueDefinition);

                     try {
                         queueDeleter.delete(queueName);
                     }
                     catch (QueueAlreadyDeletingException e) {
                         // ok
                     }
                 });


        repo.createQueue(queueDefinition);

        final long count = repo.getActiveQueues().stream().filter(i -> i.getQueueName().equals(queueName)).count();

        assertThat(count).isBetween(0L, 1L).withFailMessage("Too many active queues created");
    }
}
