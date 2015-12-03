package io.paradoxical.cassieq.dataAccess.interfaces;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public interface QueueRepository {
    void createQueue(QueueDefinition definition);

    boolean queueExists(QueueName queueName);

    Optional<QueueDefinition> getQueue(QueueName queueName);

    List<QueueDefinition> getQueues();

    default List<QueueName> getQueueNames(){
        return getQueues().stream().map(QueueDefinition::getQueueName).collect(toList());
    }
}