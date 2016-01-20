package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import io.paradoxical.cassieq.model.accounts.AccountKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static com.godaddy.logging.LoggerFactory.getLogger;

public abstract class SignedParametersBase implements RequestParameters {

    private static final Logger logger = getLogger(SignedParametersBase.class);

    protected static final Joiner signatureComponentJoiner = Joiner.on('\n')
                                                                   .skipNulls();

    @Override
    public boolean verify(final AccountKey key) throws Exception {
        if (Strings.isNullOrEmpty(getProvidedSignature())) {
            return false;
        }

        final Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        final SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");

        hmacSHA256.init(secretKeySpec);

        return verifySignature(hmacSHA256);
    }

    private boolean verifySignature(Mac hmac) {
        final String computedSignature = computeSignature(hmac);

        return computedSignature.equals(getProvidedSignature());
    }

    public String computeSignature(final Mac hmac) {
        final String signedString = getSignedString();

        final Base64.Encoder signatureEncoder = Base64.getUrlEncoder().withoutPadding();

        logger.with("signed-string", signedString).debug("Computing signature of string");

        final byte[] bytes = hmac.doFinal(signedString.getBytes());
        return signatureEncoder.encodeToString(bytes);
    }

    protected abstract String getProvidedSignature();

    protected abstract String getSignedString();
}
