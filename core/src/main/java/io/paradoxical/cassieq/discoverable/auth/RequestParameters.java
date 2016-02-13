package io.paradoxical.cassieq.discoverable.auth;

import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.util.EnumSet;

public interface RequestParameters {
    AccountName getAccountName();

    EnumSet<AuthorizationLevel> getAuthorizationLevels();

    boolean verify(VerificationContext context);
}

