package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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
    protected String getProvidedSignature() {
        return querySignature;
    }

    @Override
    public String getStringToSign() {
        return new SignedUrlParameterGenerator(accountName, authorizationLevels, startDateTime, endDateTime).getStringToSign();
    }

    @Override
    public boolean verify(final AccountKey key) {
        final boolean verified = super.verify(key);

        if (verified) {
            final DateTime now = DateTime.now(DateTimeZone.UTC);
            return startDateTime.map(now::isAfter).orElse(true) &&
                   endDateTime.map(now::isBefore).orElse(true);
        }

        return false;
    }
}

