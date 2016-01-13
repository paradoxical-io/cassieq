package io.paradoxical.cassieq.auth;

import com.google.common.base.Joiner;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.EnumSet;

@Value
@Builder
public class AuthToken {

    @NonNull
    @NotNull
    private final AccountName accountName;

    @NonNull
    @NotNull
    private final String requestMethod;

    @NonNull
    @NotNull
    private final String requestPath;

    @NonNull
    @NotNull
    private final String signature;

    @NonNull
    @NotNull
    private final EnumSet<AuthorizationLevel> authorizationLevels;

    public String getSignedString() {
        return Joiner.on("\n")
                .skipNulls()
                .join(accountName.get(),
                      requestMethod,
                      requestPath,
                      AuthorizationLevel.stringify(authorizationLevels));
    }
}
