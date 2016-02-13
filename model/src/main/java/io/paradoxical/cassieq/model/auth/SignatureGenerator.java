package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import com.google.common.io.BaseEncoding;

import javax.crypto.Mac;
import java.util.Base64;

import static com.godaddy.logging.LoggerFactory.getLogger;

/**
 * Computes a signature of a string to sign.
 */
public abstract class SignatureGenerator {

    private static final BaseEncoding signatureEncoding = BaseEncoding.base64Url().omitPadding();

    public String computeSignature(final Mac hmac) {
        final Logger logger = getLogger(SignatureGenerator.class);

        final String stringToSign = getStringToSign();

        logger.with("signed-string", stringToSign).debug("Computing signature of string");

        final byte[] bytes = hmac.doFinal(stringToSign.getBytes());

        return signatureEncoding.encode(bytes);
    }

    public abstract String getStringToSign();
}
