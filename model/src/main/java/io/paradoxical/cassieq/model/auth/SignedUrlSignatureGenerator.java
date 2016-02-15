package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.Optional;

import static com.godaddy.logging.LoggerFactory.getLogger;

/**
 * Generates a signed query param set for use for clients
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class SignedUrlSignatureGenerator extends SignatureGenerator {
    private static final Logger logger = getLogger(SignedUrlSignatureGenerator.class);

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

    @NonNull
    @NotNull
    private final Optional<QueueName> queueName;

    @Override
    public String getStringToSign() {

        return SignedStringComponentJoiner
                .join(accountName.get(),
                      AuthorizationLevel.stringify(authorizationLevels),
                      formatDateTimeForSignature(startDateTime, "startTime:"),
                      formatDateTimeForSignature(endDateTime, "endTime:"),
                      formatQueueName(queueName));
    }

    private String formatQueueName(final Optional<QueueName> queueName) {
        return queueName.map(name -> "q:" + name.get()).orElse(null);
    }

    private String formatDateTimeForSignature(final Optional<DateTime> dateTime, final String signaturePrefix) {

        return dateTime.map(SignatureGenerator::formatDateTime)
                       .map(fmt -> signaturePrefix + fmt)
                       .orElse(null);
    }
}
