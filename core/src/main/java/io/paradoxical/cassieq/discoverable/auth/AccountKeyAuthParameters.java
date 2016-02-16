package io.paradoxical.cassieq.discoverable.auth;

import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;

@Value
@Builder
public class AccountKeyAuthParameters implements RequestAuthParameters {

    @NonNull
    @NotNull
    private final AccountName accountName;

    @NonNull
    @NotNull
    private final String authKey;

    @Override
    public EnumSet<AuthorizationLevel> getAuthorizationLevels() {
        return AuthorizationLevel.All;
    }

    @Override
    public boolean verify(final VerificationContext context) {
        return authKey.equals(context.getAccountKey().get());
    }
}
