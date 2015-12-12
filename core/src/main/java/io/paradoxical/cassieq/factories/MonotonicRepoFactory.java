package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.model.QueueDefinition;

public interface MonotonicRepoFactory {
    MonotonicRepository forQueue(QueueDefinition definition);
}
