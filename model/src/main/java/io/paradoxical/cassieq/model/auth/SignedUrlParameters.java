package io.paradoxical.cassieq.model.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;
import java.util.Base64;
import java.util.EnumSet;

import static com.godaddy.logging.LoggerFactory.getLogger;

@Value
@Builder
public class SignedUrlParameters implements RequestParameters {

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

    @Override
    public boolean verify(final AccountKey key) throws Exception {
        final Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        final SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");

        hmacSHA256.init(secretKeySpec);

        return verifySignature(hmacSHA256);
    }

    private boolean verifySignature(Mac hmac) {
        final String computedSignature = computeSignature(hmac);

        return computedSignature.equals(querySignature);
    }

    public String computeSignature(final Mac hmac) {
        final String signedString = getSignedString();

        final Base64.Encoder signatureEncoder = Base64.getUrlEncoder().withoutPadding();

        logger.with("signed-string", signedString).info("Computing signature of string");

        final byte[] bytes = hmac.doFinal(signedString.getBytes());
        return signatureEncoder.encodeToString(bytes);
    }

    public String getSignedString() {
        return Joiner.on("\n")
                     .skipNulls()
                     .join(accountName.get(),
                           AuthorizationLevel.stringify(authorizationLevels));
    }
}
