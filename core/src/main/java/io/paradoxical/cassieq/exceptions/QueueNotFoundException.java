package io.paradoxical.cassieq.exceptions;

import io.paradoxical.cassieq.model.QueueName;

public class QueueNotFoundException extends EntityNotFoundException {
    public QueueNotFoundException(
            final String operation,
            final QueueName queueName) {

        super(new QueueErrorEntity(
                ErrorReason.NotFound,
                operation,
                queueName,
                "The requested queue '%s' was not found.", queueName));
    }
}

