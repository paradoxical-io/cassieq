package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.Collection;

import static com.godaddy.logging.LoggerFactory.getLogger;

@Value
@Builder
public class AuthorizedRequestCredentials {

    private static final Logger logger = getLogger(AuthorizedRequestCredentials.class);

    @NotNull
    @NonNull
    private final RequestParameters requestParameters;

    public boolean verify(Collection<AccountKey> keys) throws Exception {

        for (AccountKey key : keys) {
            if (requestParameters.verify(key)) {
                return true;
            }
        }

        return false;
    }
}
