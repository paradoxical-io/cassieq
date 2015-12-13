package io.paradoxical.cassieq.dataAccess.interfaces;

import io.paradoxical.cassieq.dataAccess.exceptions.QueueExistsError;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public interface QueueRepository {

    /**
     * Marks this queue id for deletion and makes the queue name available for creation by someone else
     * @param definition
     */
    void markForDeletion(QueueDefinition definition);

    /**
     * Attemps to create a queue with this name
     * @param definition
     * @throws QueueExistsError
     */
    void createQueue(QueueDefinition definition) throws QueueExistsError;

    /**
     * Sets the queue definition status if is possible set (some states cannot be gone back to)
     */
    boolean trySetQueueDefinitionStatus(QueueId queueId, final QueueStatus status);

    boolean queueExists(QueueName queueName);

    Optional<QueueDefinition> getQueue(QueueId queueId);

    List<QueueDefinition> getActiveQueues();

    Optional<QueueDefinition> getActiveQueue(QueueName name);

    boolean tryDeleteQueueDefinition(QueueDefinition definition);

    default List<QueueName> getQueueNames() {
        return getActiveQueues().stream().map(QueueDefinition::getQueueName).collect(toList());
    }
}