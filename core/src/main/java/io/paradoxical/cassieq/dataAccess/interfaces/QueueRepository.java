package io.paradoxical.cassieq.dataAccess.interfaces;

import io.paradoxical.cassieq.dataAccess.DeletionJob;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;
import lombok.NonNull;

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

    /**
     * Attemps to create a queue with this name
     *
     * @param definition
     */
    Optional<QueueDefinition> createQueue(QueueDefinition definition);

    boolean tryAdvanceQueueStatus(
            @NonNull QueueName queueName,
            @NonNull QueueStatus status);

    boolean deleteIfInActive(QueueName queueName);

    Optional<QueueDefinition> getQueueUnsafe(QueueName queueId);

    default List<QueueDefinition> getActiveQueues(){
        return getQueues(QueueStatus.Active);
    }

    List<QueueDefinition> getQueues(QueueStatus queueStatus);

    Optional<QueueDefinition> getActiveQueue(QueueName name);

    default List<QueueName> getQueueNames() {
        return getActiveQueues().stream().map(QueueDefinition::getQueueName).collect(toList());
    }

    void deleteCompletionJob(DeletionJob queue);
}