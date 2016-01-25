package io.paradoxical.cassieq.model.auth;

import io.paradoxical.cassieq.model.accounts.AccountKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MacProviders {

    public static Mac Hmac256(AccountKey key) {
        final String hmacSHA2561Algo = HMAC.SHA256;

        try {
            final Mac hmacSHA256 = Mac.getInstance(hmacSHA2561Algo);

            hmacSHA256.init(new SecretKeySpec(key.getBytes(), hmacSHA2561Algo));

            return hmacSHA256;
        }
        catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new AssertionError("Error initializing mac", e);
        }
    }
}
