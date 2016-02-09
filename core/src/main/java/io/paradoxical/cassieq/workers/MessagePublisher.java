package io.paradoxical.cassieq.workers;

import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.model.QueueDefinition;

public interface MessagePublisher {
    void put(QueueDefinition queueDefinition, String message, Long invisTimeSeconds) throws ExistingMonotonFoundException;
}

