package io.paradoxical.cassieq.api.client;

import com.google.common.base.Splitter;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.MacProviders;
import io.paradoxical.cassieq.model.auth.SignatureGenerator;
import io.paradoxical.cassieq.model.auth.SignedRequestSignatureGenerator;
import io.paradoxical.cassieq.model.auth.StandardAuthHeaders;
import io.paradoxical.cassieq.model.time.Clock;
import io.paradoxical.cassieq.model.time.JodaDefaultClock;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import java.util.Map;

@FunctionalInterface
public interface CassieqCredentials {

    static CassieqCredentials key(
            final AccountName accountName,
            final AccountKey accountKey) {
        return key(accountName, accountKey, new JodaDefaultClock());
    }

    static CassieqCredentials key(
            final AccountName accountName,
            final AccountKey accountKey,
            final Clock requestClock) {

        return request -> {

            final Instant now = requestClock.now();

            final SignedRequestSignatureGenerator requestParameters =
                    new SignedRequestSignatureGenerator(
                            accountName,
                            request.method(),
                            request.url().getPath(),
                            now.toDateTime(DateTimeZone.UTC));

            final String signature = requestParameters.computeSignature(MacProviders.HmacSha256(accountKey));

            final String requestTime = SignatureGenerator.formatDateTime(now);

            return request.newBuilder()
                          .header("Authorization", "Signed " + signature)
                          .header(StandardAuthHeaders.RequestTime.getHeaderName(), requestTime)
                          .build();
        };
    }

    static CassieqCredentials signedQueryString(String queryAuth) {

        final Map<String, String> queryAuthParams =
                Splitter.on('&')
                        .omitEmptyStrings()
                        .withKeyValueSeparator('=')
                        .split(queryAuth);

        return request -> {
            final HttpUrl httpUrl = request.httpUrl();

            HttpUrl.Builder newUrlBuilder = httpUrl.newBuilder();

            for (Map.Entry<String, String> entry : queryAuthParams.entrySet()) {
                newUrlBuilder = newUrlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }

            return request.newBuilder()
                          .url(newUrlBuilder.build())
                          .build();
        };
    }

    Request authorize(Request request);
}
