package io.paradoxical.cassieq.api.client;

import com.google.common.base.Splitter;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.SignedRequestParameters;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@FunctionalInterface
public interface CassieqCredentials {
    static CassieqCredentials key(final AccountName accountName, final AccountKey accountKey) throws NoSuchAlgorithmException, InvalidKeyException {
        final String hmacSHA2561Algo = "HmacSHA256";
        final Mac hmacSHA256 = Mac.getInstance(hmacSHA2561Algo);

        hmacSHA256.init(new SecretKeySpec(accountKey.getBytes(), hmacSHA2561Algo));

        final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();

        return request -> {
            final DateTime utcNow = DateTime.now(DateTimeZone.UTC);

            final SignedRequestParameters requestParameters =
                    SignedRequestParameters.builder()
                                           .accountName(accountName)
                                           .requestPath(request.url().getPath())
                                           .requestMethod(request.method())
                                           .build();

            final String signature = requestParameters.computeSignature(hmacSHA256);

            return request.newBuilder()
                          .header("Authorization", "Signed " + signature)
                          .header("x-cassieq-request-time", utcNow.toString(dateTimeFormatter))
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
