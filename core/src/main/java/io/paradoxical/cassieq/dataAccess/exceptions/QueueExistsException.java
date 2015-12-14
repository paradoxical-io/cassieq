package io.paradoxical.cassieq.dataAccess.exceptions;

import io.paradoxical.cassieq.model.QueueDefinition;

public class QueueExistsException extends Exception {
    public QueueExistsException(final QueueDefinition queueDefinition) {
        super("Queue already exists: " + queueDefinition.toString());
    }
}
