package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.Optional;

import static com.godaddy.logging.LoggerFactory.getLogger;

/**
 * Generates a signed query param set for use for clients
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class SignedUrlParameterGenerator implements SignatureGenerator {

    private static final DateTimeFormatter IsoDateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis();

    private static final Logger logger = getLogger(SignedUrlParameters.class);

    @NonNull
    @NotNull
    private final AccountName accountName;

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
    public String getStringToSign() {

        return SignatureJoiner
                .componentJoiner
                .join(accountName.get(),
                      AuthorizationLevel.stringify(authorizationLevels),
                      formatDateTimeForSignature(startDateTime, "startTime:"),
                      formatDateTimeForSignature(endDateTime, "endTime:"));
    }

    public static DateTime parseDateTime(final String dateTimeString) {
        return IsoDateTimeFormatter.parseDateTime(dateTimeString).toDateTime(DateTimeZone.UTC);
    }

    public static String formatDateTime(final DateTime dateTime) {
        return IsoDateTimeFormatter.print(dateTime);
    }

    private String formatDateTimeForSignature(final Optional<DateTime> dateTime, final String signaturePrefix) {

        return dateTime.map(SignedUrlParameterGenerator::formatDateTime)
                       .map(fmt -> signaturePrefix + fmt)
                       .orElse(null);
    }
}
