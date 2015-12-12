package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.model.QueueDefinition;

public interface PointerRepoFactory {
    PointerRepository forQueue(QueueDefinition definition);
}

