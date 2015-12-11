package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;

import java.util.Optional;

public interface DataContextFactory {
    DataContext forQueue(QueueDefinition definition);

    Optional<QueueDefinition> getDefinition(QueueName name);
}
