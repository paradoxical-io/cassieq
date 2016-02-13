package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;

import javax.crypto.Mac;
import java.util.Base64;

import static com.godaddy.logging.LoggerFactory.getLogger;

/**
 * Signs paramters
 */
public abstract class SignatureGenerator {

    public String computeSignature(final Mac hmac) {
        final Logger logger = getLogger(SignatureGenerator.class);

        final String stringToSign = getStringToSign();

        final Base64.Encoder signatureEncoder = Base64.getUrlEncoder().withoutPadding();

        logger.with("signed-string", stringToSign).debug("Computing signature of string");

        final byte[] bytes = hmac.doFinal(stringToSign.getBytes());

        return signatureEncoder.encodeToString(bytes);
    }

    public abstract String getStringToSign();
}
