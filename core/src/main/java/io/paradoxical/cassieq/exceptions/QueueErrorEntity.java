package io.paradoxical.cassieq.exceptions;

import io.paradoxical.cassieq.model.QueueName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueueErrorEntity extends ErrorEntity {
    public QueueErrorEntity(
            final ErrorReason reason,
            final String operation,
            @NonNull final QueueName queueName,
            final String message) {
        super(reason, operation, message);
        this.queueName = queueName;
    }

    public QueueErrorEntity(
            final ErrorReason reason,
            final String operation,
            final QueueName queueName,
            final String messageFormat,
            final Object ...args) {
        this(reason, operation, queueName, String.format(messageFormat, args));
    }

    public QueueErrorEntity(
            final String operation,
            @NonNull final QueueName queueName,
            final String message) {
        this(ErrorReason.Error, operation, queueName, message);
    }

    public QueueErrorEntity(
            final String operation,
            final QueueName queueName,
            final String messageFormat,
            final Object ...args) {
        this(operation, queueName, String.format(messageFormat, args));
    }

    @NonNull
    private final QueueName queueName;
}
