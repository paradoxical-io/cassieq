package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.workers.reader.InvisStrategy;

public interface InvisStrategyFactory {
    InvisStrategy forQueue(QueueDefinition definition);
}
