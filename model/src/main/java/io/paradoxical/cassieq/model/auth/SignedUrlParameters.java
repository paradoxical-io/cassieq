package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;

import static com.godaddy.logging.LoggerFactory.getLogger;

/**
 * Represents the parsed signed URL pamaters from a request
 */
@EqualsAndHashCode(callSuper = false)
@Value
@Builder
public class SignedUrlParameters extends SignedParametersBase implements RequestParameters {

    private static final Logger logger = getLogger(SignedUrlParameters.class);

    @NonNull
    @NotNull
    private final AccountName accountName;

    @NonNull
    @NotNull
    private final String querySignature;

    @NonNull
    @NotNull
    private final EnumSet<AuthorizationLevel> authorizationLevels;

    @Override
    protected String getProvidedSignature() {
        return querySignature;
    }

    @Override
    public String getStringToSign() {
        return new SignedUrlParameterGenerator(accountName, authorizationLevels).getStringToSign();
    }
}

