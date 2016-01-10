package io.paradoxical.cassieq.dataAccess.interfaces;

import io.paradoxical.cassieq.dataAccess.DeletionJob;
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
     *
     * @param definition
     */
    Optional<DeletionJob> tryMarkForDeletion(QueueDefinition definition);

    Optional<Long> getQueueSize(QueueDefinition definition);

    /**
     * Attemps to create a queue with this name
     *
     * @param definition
     */
    Optional<QueueDefinition> createQueue(QueueDefinition definition);

    boolean tryAdvanceQueueStatus(QueueName queueName, QueueStatus status);

    Optional<QueueDefinition> getQueueUnsafe(QueueName queueId);

    default List<QueueDefinition> getActiveQueues() {
        return getQueues(QueueStatus.Active);
    }

    List<QueueDefinition> getQueues(QueueStatus queueStatus);

    Optional<QueueDefinition> getActiveQueue(QueueName name);

    default List<QueueName> getQueueNames() {
        return getActiveQueues().stream().map(QueueDefinition::getQueueName).collect(toList());
    }

    void deleteCompletionJob(DeletionJob queue);

    void deleteQueueStats(QueueId id);
}