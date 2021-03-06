package io.paradoxical.cassieq.discoverable.auth;

import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.SignatureGenerator;
import io.paradoxical.cassieq.model.auth.SignedRequestSignatureGenerator;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.DateTime;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;

@EqualsAndHashCode(callSuper = false)
@Value
@Builder
public class SignedRequestAuthParameters extends SignedAuthParametersBase implements RequestAuthParameters {

    @NonNull
    @NotNull
    private final AccountName accountName;

    @NonNull
    @NotNull
    private final String requestMethod;

    @NonNull
    @NotNull
    private final String requestPath;

    @NonNull
    @NotNull
    private final DateTime requestTime;

    private final String providedSignature;

    @Override
    public EnumSet<AuthorizationLevel> getAuthorizationLevels() {
        return AuthorizationLevel.All;
    }

    @Override
    protected SignatureGenerator getSignatureGenerator() {
        return new SignedRequestSignatureGenerator(accountName, requestMethod, requestPath, requestTime);
    }

    @Override
    public boolean verify(final VerificationContext context) {

        return super.verify(context) &&
               context.requestTimeWithinAllowances(requestTime);
    }
}
