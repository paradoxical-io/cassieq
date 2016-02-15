package io.paradoxical.cassieq.model.auth;

import com.google.common.base.CharMatcher;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.joda.time.DateTime;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = false)
@Value
public class SignedRequestSignatureGenerator extends SignatureGenerator {
    private static final CharMatcher slashMatcher = CharMatcher.is('/');

    @NonNull
    @NotNull
    private final AccountName accountName;

    @NonNull
    @NotNull
    private final String requestMethod;

    @NonNull
    @NotNull
    private final String requestPath;

    @NonNull
    @NotNull
    private final DateTime requestTime;

    @Override
    public String getStringToSign() {

        return SignedStringComponentJoiner
                .join(accountName.get(), requestMethod, canonicalizeRequestPath(requestPath), formatDateTime(requestTime));
    }

    private static String canonicalizeRequestPath(final String requestPath) {
        return "/" + slashMatcher.trimFrom(requestPath);
    }
}
