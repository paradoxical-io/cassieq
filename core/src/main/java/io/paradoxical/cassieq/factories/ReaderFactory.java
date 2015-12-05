package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.workers.reader.Reader;

public interface ReaderFactory {
    Reader forQueue(QueueDefinition definition);
}
