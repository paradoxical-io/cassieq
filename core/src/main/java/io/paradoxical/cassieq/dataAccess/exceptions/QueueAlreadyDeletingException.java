package io.paradoxical.cassieq.dataAccess.exceptions;

import io.paradoxical.cassieq.model.QueueDefinition;

public class QueueAlreadyDeletingException extends Exception {
    public QueueAlreadyDeletingException(final QueueDefinition queueDefinition) {
        super("Queue already deleting: " + queueDefinition.toString());
    }
}
