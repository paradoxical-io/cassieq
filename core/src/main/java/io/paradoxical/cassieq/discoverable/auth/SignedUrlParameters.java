package io.paradoxical.cassieq.discoverable.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.SignatureGenerator;
import io.paradoxical.cassieq.model.auth.SignedUrlSignatureGenerator;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.Optional;

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

    @NonNull
    @NotNull
    private final Optional<DateTime> startDateTime;

    @NonNull
    @NotNull
    private final Optional<DateTime> endDateTime;

    @Override
    public boolean verify(final VerificationContext context) {
        final boolean verified = super.verify(context);

        if (verified) {
            final Instant now = context.getClock().now();
            return startDateTime.map(now::isAfter).orElse(true) &&
                   endDateTime.map(now::isBefore).orElse(true);
        }

        return false;
    }

    @Override
    protected SignatureGenerator getSignatureGenerator() {
        return new SignedUrlSignatureGenerator(accountName, authorizationLevels, startDateTime, endDateTime);
    }

    @Override
    protected String getProvidedSignature() {
        return querySignature;
    }
}

