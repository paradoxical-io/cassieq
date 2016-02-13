package io.paradoxical.cassieq.discoverable.auth;

import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.time.Clock;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
public class VerificationContext {

    @NotNull
    @NonNull
    private final AccountKey accountKey;

    @NotNull
    @NonNull
    private final Clock clock;

    @NotNull
    @NonNull
    private final Optional<QueueName> queueName;
}
