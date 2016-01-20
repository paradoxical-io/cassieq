package io.paradoxical.cassieq.model.auth;

import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;

@Value
@Builder
public class AccountKeyParameters implements RequestParameters {

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
    public boolean verify(final AccountKey key) {
        return authKey.equals(key.get());
    }
}
