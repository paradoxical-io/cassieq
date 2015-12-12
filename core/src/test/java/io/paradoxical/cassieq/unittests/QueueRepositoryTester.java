package io.paradoxical.cassieq.unittests;

import com.google.inject.Injector;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueExistsError;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import org.junit.Test;

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
    public void delete_queue() throws QueueExistsError {
        final Injector defaultInjector = getDefaultInjector();

        final QueueRepository repo = defaultInjector.getInstance(QueueRepository.class);

        final QueueName queueName = QueueName.valueOf("delete_queue");

        final QueueDefinition queueDefinition = QueueDefinition.builder().queueName(queueName).build();

        repo.createQueue(queueDefinition);

        assertThat(repo.queueExists(queueName)).isEqualTo(true);

        assertThat(repo.getQueueNames()).contains(queueName);

        repo.deleteQueueDefinition(queueDefinition);

        assertThat(repo.queueExists(queueName)).isEqualTo(false);

        assertThat(repo.getQueueNames()).doesNotContain(queueName);
    }
}
