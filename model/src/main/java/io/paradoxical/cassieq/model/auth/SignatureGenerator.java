package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.accounts.AccountKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static com.godaddy.logging.LoggerFactory.getLogger;

/**
 * Signs paramters
 */
public interface SignatureGenerator {

    default String computeSignature(AccountKey key) throws InvalidKeyException, NoSuchAlgorithmException {
        final String hmacSHA2561Algo = "HmacSHA256";
        final Mac hmacSHA256 = Mac.getInstance(hmacSHA2561Algo);

        hmacSHA256.init(new SecretKeySpec(key.getBytes(), hmacSHA2561Algo));

        return computeSignature(hmacSHA256);
    }

    default String computeSignature(final Mac hmac) {
        final Logger logger = getLogger(SignatureGenerator.class);

        final String stringToSign = getStringToSign();

        final Base64.Encoder signatureEncoder = Base64.getUrlEncoder().withoutPadding();

        logger.with("signed-string", stringToSign).debug("Computing signature of string");

        final byte[] bytes = hmac.doFinal(stringToSign.getBytes());

        return signatureEncoder.encodeToString(bytes);
    }

    String getStringToSign();
}
