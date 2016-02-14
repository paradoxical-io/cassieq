package io.paradoxical.cassieq.exceptions;

import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.KeyName;
import lombok.Getter;

public class AccountKeyNotFoundException extends EntityNotFoundException {
    public AccountKeyNotFoundException(
            final String operation,
            final KeyName keyName) {
        super(new ErrorEntity(ErrorReason.NotFound,
                              operation,
                              "The requested account key of '%s' was not found.", keyName) {
            @Getter
            private final KeyName key = keyName;
        });
    }
}

