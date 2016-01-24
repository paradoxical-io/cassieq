package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import io.paradoxical.cassieq.model.accounts.AccountName;
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
    private static final CharMatcher slashMatcher = CharMatcher.is('/');

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

    public String getStringToSign() {
        return Joiner.on("\n")
                     .skipNulls()
                     .join(accountName.get(),
                           requestMethod,
                           "/" + slashMatcher.trimFrom(requestPath));
    }
}
