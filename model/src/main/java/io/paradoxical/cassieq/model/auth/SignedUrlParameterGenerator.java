package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;

import static com.godaddy.logging.LoggerFactory.getLogger;

/**
 * Generates a signed query param set for use for clients
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class SignedUrlParameterGenerator implements SignatureGenerator {

    private static final Logger logger = getLogger(SignedUrlParameters.class);

    @NonNull
    @NotNull
    private final AccountName accountName;

    @NonNull
    @NotNull
    private final EnumSet<AuthorizationLevel> authorizationLevels;

    @Override
    public String getStringToSign() {
        return SignatureJoiner
                .componentJoiner
                .join(accountName.get(),
                      AuthorizationLevel.stringify(authorizationLevels));
    }
}
