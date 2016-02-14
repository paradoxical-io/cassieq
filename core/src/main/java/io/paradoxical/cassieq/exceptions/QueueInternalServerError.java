package io.paradoxical.cassieq.exceptions;

import io.paradoxical.cassieq.model.QueueName;

public class QueueInternalServerError extends InternalSeverError {
    public QueueInternalServerError(
            final String operation,
            final QueueName queueName,
            final Throwable error) {

        super(new QueueErrorEntity(operation, queueName, error.getMessage()), error);
    }
}
