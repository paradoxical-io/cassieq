package io.paradoxical.cassieq.exceptions;

import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Getter;

public class AccountNotFoundException extends EntityNotFoundException {
    public AccountNotFoundException(
            final String operation,
            final AccountName accountName) {
        super(new ErrorEntity(ErrorReason.NotFound, operation, "The requested account '%s' was not found.", accountName) {
            @Getter
            private final AccountName account = accountName;
        });
    }
}
