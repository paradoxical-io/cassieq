package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.Strings;
import io.paradoxical.cassieq.model.accounts.AccountKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.godaddy.logging.LoggerFactory.getLogger;

public abstract class SignedParametersBase implements RequestParameters, SignatureGenerator {

    private static final Logger logger = getLogger(SignedParametersBase.class);

    @Override
    public boolean verify(final AccountKey key) throws Exception {
        if (Strings.isNullOrEmpty(getProvidedSignature())) {
            return false;
        }

        final Mac hmacSHA256 = Mac.getInstance(HMAC.SHA256);
        final SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC.SHA256);

        hmacSHA256.init(secretKeySpec);

        return verifySignature(hmacSHA256);
    }

    private boolean verifySignature(Mac hmac) {
        final String computedSignature = computeSignature(hmac);

        return computedSignature.equals(getProvidedSignature());
    }


    protected abstract String getProvidedSignature();
}
