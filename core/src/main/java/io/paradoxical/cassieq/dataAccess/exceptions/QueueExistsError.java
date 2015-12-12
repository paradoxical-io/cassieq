package io.paradoxical.cassieq.dataAccess.exceptions;

import io.paradoxical.cassieq.model.QueueDefinition;

public class QueueExistsError extends Exception {
    public QueueExistsError(final QueueDefinition queueDefinition) {
        super("Queue already exists: " + queueDefinition.toString());
    }
}
