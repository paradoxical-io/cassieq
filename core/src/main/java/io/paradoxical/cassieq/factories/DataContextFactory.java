package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.model.QueueDefinition;

public interface DataContextFactory {
    DataContext forQueue(QueueDefinition definition);
}
