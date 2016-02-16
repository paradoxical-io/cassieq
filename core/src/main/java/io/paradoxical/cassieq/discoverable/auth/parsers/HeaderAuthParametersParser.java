package io.paradoxical.cassieq.discoverable.auth.parsers;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.paradoxical.cassieq.discoverable.auth.AccountKeyAuthParameters;
import io.paradoxical.cassieq.discoverable.auth.RequestAuthParameters;
import io.paradoxical.cassieq.discoverable.auth.SignedRequestAuthParameters;
import io.paradoxical.cassieq.discoverable.auth.parsers.AuthParametersParser;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.SignatureGenerator;
import io.paradoxical.cassieq.model.auth.StandardAuthHeaders;
import org.joda.time.DateTime;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

public class HeaderAuthParametersParser implements AuthParametersParser<RequestAuthParameters> {

    private final String keyAuthHeaderPrefix;
    private final String signedRequestHeaderPrefix;

    public HeaderAuthParametersParser(final String keyAuthHeaderPrefix, final String signedRequestHeaderPrefix) {
        this.keyAuthHeaderPrefix = keyAuthHeaderPrefix;
        this.signedRequestHeaderPrefix = signedRequestHeaderPrefix;
    }

    public java.util.Optional<RequestAuthParameters> tryParse(final ContainerRequestContext requestContext, final AccountName accountName) {
        return java.util.Optional.ofNullable(parseHeaderRequestAuthParameters(requestContext, accountName));
    }

    private RequestAuthParameters parseHeaderRequestAuthParameters(final ContainerRequestContext requestContext, final AccountName accountName) {
        final String header = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!Strings.isNullOrEmpty(header)) {
            final Splitter splitter = Splitter.on(' ').limit(2).trimResults();
            final List<String> components = splitter.splitToList(header);

            if (components.size() == 2) {
                final String authScheme = components.get(0);

                // first check if the request is using a raw key scheme
                if (keyAuthHeaderPrefix.equalsIgnoreCase(authScheme)) {
                    return AccountKeyAuthParameters.builder()
                                                   .accountName(accountName)
                                                   .authKey(components.get(1))
                                                   .build();
                }

                // then check if the request is using a "signed" (or what ever we're configured to expect via prefix) request scheme,
                if (signedRequestHeaderPrefix.equalsIgnoreCase(authScheme)) {
                    final DateTime requestTime = parseRequestTimeHeader(requestContext.getHeaders());

                    return SignedRequestAuthParameters.builder()
                                                      .accountName(accountName)
                                                      .requestMethod(requestContext.getMethod())
                                                      .providedSignature(components.get(1))
                                                      .requestPath(requestContext.getUriInfo().getPath())
                                                      .requestTime(requestTime)
                                                      .build();
                }
            }
        }

        return null;
    }

    private DateTime parseRequestTimeHeader(final MultivaluedMap<String, String> headers) {

        final String requestTimeHeaderValue = headers.getFirst(StandardAuthHeaders.RequestTime.getHeaderName());

        if (Strings.isNullOrEmpty(requestTimeHeaderValue)) {
            return new DateTime(0); // min value
        }

        return SignatureGenerator.parseDateTime(requestTimeHeaderValue);
    }
}
