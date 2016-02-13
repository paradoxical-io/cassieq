package io.paradoxical.cassieq.model.accounts;

import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.DateTime;

import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.Set;

@Value
@Builder(builderClassName = "Builder")
public class GetAuthQueryParamsRequest {
    @NotNull
    private AccountName accountName;

    @NotNull
    private KeyName keyName;

    @NotNull
    @lombok.Singular
    private Set<AuthorizationLevel> levels;

    private Optional<DateTime> startTime;

    private Optional<DateTime> endTime;

    private Optional<QueueName> queueName;

    @java.beans.ConstructorProperties({ "accountName", "keyName", "levels", "startTime", "endTime", "queueName" })
    private GetAuthQueryParamsRequest(
            @NonNull final AccountName accountName,
            @NonNull final KeyName keyName,
            @NonNull final Set<AuthorizationLevel> levels,
            final Optional<DateTime> startTime,
            final Optional<DateTime> endTime,
            final Optional<QueueName> queueName) {
        this.accountName = accountName;
        this.keyName = keyName;
        this.levels = levels;
        this.startTime = startTime == null ? Optional.empty() : startTime;
        this.endTime = endTime == null ? Optional.empty() : endTime;
        this.queueName = queueName == null ? Optional.empty() : queueName;
    }

    public static class Builder {

        public Builder keyName(@NonNull WellKnownKeyNames knownKeyName) {
            return keyName(knownKeyName.getKeyName());
        }

        public Builder keyName(@NonNull KeyName keyName) {
            this.keyName = keyName;
            return this;
        }

        public Builder endTime(@NonNull DateTime endTime) {
            this.endTime = Optional.of(endTime);
            return this;
        }

        public Builder startTime(@NonNull DateTime startTime) {
            this.startTime = Optional.of(startTime);
            return this;
        }

        public Builder queueName(@NonNull QueueName queueName) {
            this.queueName = Optional.of(queueName);
            return this;
        }
    }
}
