package io.paradoxical.cassieq.discoverable.auth;

import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.time.Clock;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Value
public class VerificationContext {

    @NotNull
    @NonNull
    private final AccountName accountName;

    @NotNull
    @NonNull
    private final AccountKey accountKey;

    @NotNull
    @NonNull
    private final Clock clock;

    @NotNull
    @NonNull
    private final Optional<QueueName> queueName;

    @NotNull
    @NonNull
    private final Duration allowedClockSkew;

    public boolean requestTimeWithinAllowances(final DateTime requestTime) {
        final Instant now = clock.now();

        return now.minus(allowedClockSkew).isBefore(requestTime) &&
               now.plus(allowedClockSkew).isAfter(requestTime);
    }
}
