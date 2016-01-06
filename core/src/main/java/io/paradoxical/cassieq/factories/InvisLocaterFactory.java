package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.workers.reader.InvisLocatorImpl;

public interface InvisLocaterFactory {
    InvisLocatorImpl forQueue(QueueDefinition definition);
}
