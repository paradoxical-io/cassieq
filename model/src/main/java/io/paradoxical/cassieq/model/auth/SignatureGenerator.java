package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.Joiner;
import com.google.common.io.BaseEncoding;
import lombok.Getter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.crypto.Mac;
import java.util.Base64;

import static com.godaddy.logging.LoggerFactory.getLogger;

/**
 * Computes a signature of a string to sign.
 */
public abstract class SignatureGenerator {
    private static final DateTimeFormatter IsoDateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis();

    private static final BaseEncoding signatureEncoding = BaseEncoding.base64Url().omitPadding();

    protected static final Joiner SignedStringComponentJoiner = Joiner.on('\n').skipNulls();

    public static DateTime parseDateTime(final String dateTimeString) {
        return IsoDateTimeFormatter.parseDateTime(dateTimeString)
                                   .toDateTime(DateTimeZone.UTC);
    }

    public static String formatDateTime(final ReadableInstant dateTime) {
        return IsoDateTimeFormatter.print(dateTime);
    }

    public String computeSignature(final Mac hmac) {
        final Logger logger = getLogger(SignatureGenerator.class);

        final String stringToSign = getStringToSign();

        logger.with("signed-string", stringToSign).debug("Computing signature of string");

        final byte[] bytes = hmac.doFinal(stringToSign.getBytes());

        return signatureEncoding.encode(bytes);
    }

    public abstract String getStringToSign();
}
