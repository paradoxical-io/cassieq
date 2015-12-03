package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.model.QueueName;

public interface PointerRepoFactory {
    PointerRepository forQueue(QueueName queueName);
}

