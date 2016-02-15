package io.paradoxical.cassieq.discoverable.auth;

import com.google.common.collect.ImmutableCollection;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.time.Clock;
import lombok.Value;
import org.joda.time.Duration;

import java.util.Collection;

@Value
public class CredentialsVerificationContext {
    private final ImmutableCollection<AccountKey> keys;
    private final Clock clock;
    private final Duration allowedClockSkew;
}
