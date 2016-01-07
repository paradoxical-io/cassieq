package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.workers.reader.InvisLocator;

public interface InvisLocaterFactory {
    InvisLocator forQueue(QueueDefinition definition);
}
