package io.paradoxical.cassieq.discoverable.auth;

import com.google.common.collect.ImmutableCollection;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.time.Clock;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.Duration;

import javax.validation.constraints.NotNull;
import java.util.Collection;

@Value
public class CredentialsVerificationContext {

    @NotNull
    @NonNull
    private final ImmutableCollection<AccountKey> keys;

    @NotNull
    @NonNull
    private final Clock clock;

    @NotNull
    @NonNull
    private final Duration allowedClockSkew;
}
