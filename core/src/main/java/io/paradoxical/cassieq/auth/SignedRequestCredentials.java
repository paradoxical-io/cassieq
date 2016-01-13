package io.paradoxical.cassieq.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

import static com.godaddy.logging.LoggerFactory.getLogger;

@Value
@Builder
public class SignedRequestCredentials {

    private static final Logger logger = getLogger(SignedRequestCredentials.class);
    private static final Base64.Encoder signatureEncoder = Base64.getUrlEncoder().withoutPadding();

    @NonNull
    @NotNull
    private final AccountName accountName;

    @NonNull
    @NotNull
    private final String requestMethod;

    @NonNull
    @NotNull
    private final String requestPath;

    private final String signature;

    private final String authKey;

    @NonNull
    @NotNull
    private final EnumSet<AuthorizationLevel> authorizationLevels;


    private boolean hasSignature() {
        return !Strings.isNullOrEmpty(signature);
    }

    public boolean verify(Set<AccountKey> keys) throws NoSuchAlgorithmException, InvalidKeyException {
        final Mac hmacSHA256 = Mac.getInstance("HmacSHA256");

        for (AccountKey key : keys) {

            if (!Strings.isNullOrEmpty(signature)) {

                final Base64.Decoder urlDecoder = Base64.getUrlDecoder();
                final SecretKeySpec secretKeySpec = new SecretKeySpec(urlDecoder.decode(key.get()), "HmacSHA256");

                hmacSHA256.reset();
                hmacSHA256.init(secretKeySpec);

                if (verifySignature(hmacSHA256)) {
                    return true;
                }
            }

            if (!Strings.isNullOrEmpty(authKey) &&
                authKey.equals(key.get())) {
                return true;
            }
        }

        return false;
    }

    private boolean verifySignature(Mac hmac) {
        final String computedSignature = computeSignature(hmac);

        return computedSignature.equals(signature);
    }

    public String computeSignature(final Mac hmac) {
        final byte[] bytes = hmac.doFinal(getSignedString().getBytes());
        return signatureEncoder.encodeToString(bytes);
    }

    public String getSignedString() {
        return Joiner.on("\n")
                     .skipNulls()
                     .join(accountName.get(),
                           requestMethod,
                           requestPath,
                           AuthorizationLevel.stringify(authorizationLevels));
    }
}
