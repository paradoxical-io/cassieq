package io.paradoxical.cassieq.unittests;

import com.google.inject.Injector;
import io.paradoxical.cassieq.dataAccess.QueueRepositoryImpl;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueExistsError;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class QueueRepositoryTester extends TestBase {
    @Test
    public void queue_operations() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("queue_operations");

        assertThat(repo.getQueue(queueName).isPresent()).isEqualTo(false);

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueue(queueName).isPresent()).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);
    }

    @Test
    public void cannot_create_same_queue_twice() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("cannot_create_same_queue_twice");

        assertThat(repo.getQueue(queueName).isPresent()).isEqualTo(false);

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueue(queueName).isPresent()).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);

        repo.createQueue(queueDefinition);

        fail("Queue exists error not thrown");
    }

    @Test
    public void queue_defintion_once_in_status_cant_move_backwards() throws QueueExistsError {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepositoryImpl repo = (QueueRepositoryImpl)defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("queue_defintion_once_in_status_cant_move_backwards");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueue(queueName).isPresent()).isEqualTo(true);

        assertThat(repo.trySetQueueDefinitionStatus(queueDefinition.getQueueName(), QueueStatus.Deleting)).isTrue();

        assertThat(repo.trySetQueueDefinitionStatus(queueDefinition.getQueueName(), QueueStatus.Inactive)).isTrue();

        assertThat(repo.trySetQueueDefinitionStatus(queueDefinition.getQueueName(), QueueStatus.Active)).isFalse();
    }

    @Test
    public void can_create_new_queue_while_old_queue_is_deleting() throws QueueExistsError {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("delete_queue");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueue(queueName).isPresent()).isEqualTo(true);

        repo.markForDeletion(queueDefinition);

        // should be able to create a new defintion here
        repo.createQueue(queueDefinition);
    }

    @Test
    public void delete_queue() throws QueueExistsError {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("delete_queue");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.getQueue(queueName).isPresent()).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);

        repo.tryDeleteQueueDefinition(queueDefinition);

        assertThat(repo.getQueue(queueName).isPresent()).isEqualTo(false);

        assertThat(repo.getQueueNames()).doesNotContain(queueName);
    }

    @Test
    public void only_one_active_ever() throws QueueExistsError {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("only_one_active_ever");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        IntStream.range(0, 1000)
                 .parallel()
                 .forEach(i -> {
                     repo.createQueue(queueDefinition);

                     repo.tryDeleteQueueDefinition(queueDefinition);
                 });


        repo.createQueue(queueDefinition);


        final long count = repo.getActiveQueues().stream().filter(i -> i.getQueueName().equals(queueName)).count();

        assertThat(count).isBetween(0L, 1L).withFailMessage("Too many active queues created");
    }
}
