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
    void createQueue(QueueDefinition definition) throws QueueExistsError;

    void setQueueStatus(QueueId queueId, final QueueStatus status);

    boolean queueExists(QueueName queueName);

    Optional<QueueDefinition> getQueue(QueueId queueId);

    List<QueueDefinition> getActiveQueues();

    Optional<QueueDefinition> getActiveQueue(QueueName name);

    boolean tryDeleteQueueDefinition(QueueDefinition definition);

    default List<QueueName> getQueueNames() {
        return getActiveQueues().stream().map(QueueDefinition::getQueueName).collect(toList());
    }
}