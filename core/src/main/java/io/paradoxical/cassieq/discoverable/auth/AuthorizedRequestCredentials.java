package io.paradoxical.cassieq.discoverable.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.time.Clock;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.Collection;
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
    private final RequestParameters requestParameters;

    public boolean verify(Collection<AccountKey> keys, Clock clock, Optional<QueueName> queueName) throws Exception {

        for (AccountKey key : keys) {
            if (requestParameters.verify(new VerificationContext(key, clock, queueName))) {
                return true;
            }
        }

        return false;
    }
}
