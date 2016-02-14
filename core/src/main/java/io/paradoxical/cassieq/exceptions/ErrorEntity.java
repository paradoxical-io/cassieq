package io.paradoxical.cassieq.exceptions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ErrorEntity {

    public ErrorEntity(
            @NonNull final String operation,
            @NonNull final String message){
        this(ErrorReason.Error, operation, message);
    }

    public ErrorEntity(
            @NonNull final String operation,
            @NonNull final String messageFormat,
            final Object ...args) {
        this(operation, String.format(messageFormat, args));
    }

    public ErrorEntity(
            @NonNull final ErrorReason reason,
            @NonNull final String operation,
            @NonNull final String messageFormat,
            final Object ...args) {
        this(reason, operation, String.format(messageFormat, args));
    }

    @NonNull
    private final ErrorReason reason;

    @NonNull
    private final String operation;

    @NonNull
    private final String message;
}
