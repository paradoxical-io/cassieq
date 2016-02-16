package io.paradoxical.cassieq.discoverable.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.Strings;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.auth.MacProviders;
import io.paradoxical.cassieq.model.auth.SignatureGenerator;

import javax.crypto.Mac;

import static com.godaddy.logging.LoggerFactory.getLogger;

public abstract class SignedAuthParametersBase implements RequestAuthParameters {

    private static final Logger logger = getLogger(SignedAuthParametersBase.class);

    @Override
    public boolean verify(final VerificationContext context)  {
        if (Strings.isNullOrEmpty(getProvidedSignature())) {
            return false;
        }

        return verifySignature(MacProviders.HmacSha256(context.getAccountKey()));
    }

    private boolean verifySignature(Mac hmac) {
        final String computedSignature = getSignatureGenerator().computeSignature(hmac);

        return computedSignature.equals(getProvidedSignature());
    }

    protected abstract SignatureGenerator getSignatureGenerator();

    protected abstract String getProvidedSignature();
}
