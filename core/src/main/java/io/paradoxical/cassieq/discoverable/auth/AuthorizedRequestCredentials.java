package io.paradoxical.cassieq.discoverable.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.time.Clock;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.Duration;

import javax.validation.constraints.NotNull;
import java.util.Optional;

import static com.godaddy.logging.LoggerFactory.getLogger;

@Value
@Builder
public class AuthorizedRequestCredentials {

    private static final Logger logger = getLogger(AuthorizedRequestCredentials.class);

    @NotNull
    @NonNull
    private final AccountName accountName;

    @NotNull
    @NonNull
    private final Optional<QueueName> queueName;

    @NotNull
    @NonNull
    private final RequestAuthParameters requestAuthParameters;

    public boolean verify(final CredentialsVerificationContext credentialsVerificationContext) throws Exception {

        final Clock clock = credentialsVerificationContext.getClock();
        final Duration allowedClockSkew = credentialsVerificationContext.getAllowedClockSkew();

        for (AccountKey key : credentialsVerificationContext.getKeys()) {

            final VerificationContext verificationContext =
                    new VerificationContext(accountName, key, clock, queueName, allowedClockSkew);

            if (requestAuthParameters.verify(verificationContext)) {
                return true;
            }
        }

        return false;
    }
}

