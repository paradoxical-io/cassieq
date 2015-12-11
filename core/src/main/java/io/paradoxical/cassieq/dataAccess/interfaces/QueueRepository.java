package io.paradoxical.cassieq.dataAccess.interfaces;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public interface QueueRepository {
    void createQueue(QueueDefinition definition);

    void setQueueStatus(QueueName queueName, final QueueStatus status);

    boolean queueExists(QueueName queueName);

    Optional<QueueDefinition> getQueue(QueueName queueName);

    List<QueueDefinition> getQueues();

    void deleteQueueDefinition(QueueName queueName);

    default List<QueueName> getQueueNames(){
        return getQueues().stream().map(QueueDefinition::getQueueName).collect(toList());
    }
}