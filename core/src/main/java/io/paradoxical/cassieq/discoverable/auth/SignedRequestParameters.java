package io.paradoxical.cassieq.discoverable.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.SignatureGenerator;
import io.paradoxical.cassieq.model.auth.SignedRequestSignatureGenerator;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;

import static com.godaddy.logging.LoggerFactory.getLogger;

@EqualsAndHashCode(callSuper = false)
@Value
@Builder
public class SignedRequestParameters extends SignedParametersBase implements RequestParameters {

    private static final Logger logger = getLogger(SignedRequestParameters.class);

    @NonNull
    @NotNull
    private final AccountName accountName;

    @NonNull
    @NotNull
    private final String requestMethod;

    @NonNull
    @NotNull
    private final String requestPath;

    private final String providedSignature;

    @Override
    public EnumSet<AuthorizationLevel> getAuthorizationLevels() {
        return AuthorizationLevel.All;
    }

    @Override
    protected SignatureGenerator getSignatureGenerator() {
        return new SignedRequestSignatureGenerator(accountName, requestMethod, requestPath);
    }
}
