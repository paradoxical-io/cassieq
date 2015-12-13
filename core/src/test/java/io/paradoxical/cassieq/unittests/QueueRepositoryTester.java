package io.paradoxical.cassieq.unittests;

import com.google.inject.Injector;
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

        assertThat(repo.queueExists(queueName)).isEqualTo(false);

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.queueExists(queueName)).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);
    }

    @Test
    public void cannot_create_same_queue_twice() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("cannot_create_same_queue_twice");

        assertThat(repo.queueExists(queueName)).isEqualTo(false);

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.queueExists(queueName)).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);

        try {
            repo.createQueue(queueDefinition);
        }
        catch (QueueExistsError ex) {
            return;
        }

        fail("Queue exists error not thrown");
    }

    @Test
    public void queue_defintion_once_in_status_cant_move_backwards() throws QueueExistsError {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("queue_defintion_once_in_status_cant_move_backwards");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.queueExists(queueName)).isEqualTo(true);

        assertThat(repo.trySetQueueDefinitionStatus(queueDefinition.getId(), QueueStatus.Deleting)).isTrue();

        assertThat(repo.trySetQueueDefinitionStatus(queueDefinition.getId(), QueueStatus.Inactive)).isTrue();

        assertThat(repo.trySetQueueDefinitionStatus(queueDefinition.getId(), QueueStatus.Active)).isFalse();
    }

    @Test
    public void delete_queue() throws QueueExistsError {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("delete_queue");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.queueExists(queueName)).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);

        repo.tryDeleteQueueDefinition(queueDefinition);

        assertThat(repo.queueExists(queueName)).isEqualTo(false);

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
                     try {
                         repo.createQueue(queueDefinition);

                         repo.tryDeleteQueueDefinition(queueDefinition);
                     }
                     catch (QueueExistsError queueExistsError) {
                     }
                 });

        try {
            repo.createQueue(queueDefinition);
        }
        catch (QueueExistsError ex) {
        }

        final long count = repo.getActiveQueues().stream().filter(i -> i.getQueueName().equals(queueName)).count();

        assertThat(count).isBetween(0L, 1L).withFailMessage("Too many active queues created");
    }
}
