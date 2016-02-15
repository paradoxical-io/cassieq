package io.paradoxical.cassieq.discoverable.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.SignatureGenerator;
import io.paradoxical.cassieq.model.auth.SignedUrlSignatureGenerator;
import io.paradoxical.cassieq.model.auth.StandardAuthHeaders;
import lombok.Builder;
import org.joda.time.DateTime;

import javax.annotation.Priority;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.security.Signature;
import java.util.EnumSet;
import java.util.List;

import static com.godaddy.logging.LoggerFactory.getLogger;

@AccountAuth
@Priority(Priorities.AUTHENTICATION)
@Builder(builderClassName = "Builder")
public class SignedRequestAuthenticationFilter<TPrincipal extends Principal> extends AuthFilter<AuthorizedRequestCredentials, TPrincipal> {
    private static final Logger logger = getLogger(SignedRequestAuthenticationFilter.class);

    private final String accountNamePathParameter;

    private final String queueNamePathParameter;

    private final String keyAuthPrefix;

    public SignedRequestAuthenticationFilter(
            final String accountNamePathParameter,
            final String queueNamePathParameter,
            final String keyAuthPrefix) {
        this.accountNamePathParameter = accountNamePathParameter;
        this.queueNamePathParameter = queueNamePathParameter;
        this.keyAuthPrefix = keyAuthPrefix;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        try {

            final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
            if (pathParameters.containsKey(accountNamePathParameter)) {

                final AccountName accountName = AccountName.valueOf(pathParameters.getFirst(accountNamePathParameter));

                final java.util.Optional<QueueName> queueName = tryGetQueueName(pathParameters, queueNamePathParameter);

                final RequestParameters headerRequestParameters = parseHeaderRequestParameters(requestContext, accountName);

                final SignedUrlParameters signedUrlParameters = parseSignedUrlParameters(requestContext, accountName, queueName);

                if (headerRequestParameters == null && signedUrlParameters == null) {
                    throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
                }

                // the request params we'll use to auth, with priority from header vs query param
                final RequestParameters requestParameters = MoreObjects.firstNonNull(headerRequestParameters, signedUrlParameters);

                final AuthorizedRequestCredentials credentials =
                        AuthorizedRequestCredentials.builder()
                                                    .requestParameters(requestParameters)
                                                    .queueName(queueName)
                                                    .accountName(accountName)
                                                    .build();

                if (setPrincipal(requestContext, credentials)) {
                    return;
                }
            }
            else {
                return;
            }
        }
        catch (IllegalArgumentException e) {
            logger.warn("Error decoding credentials", e);
        }

        throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
    }

    private java.util.Optional<QueueName> tryGetQueueName(
            final MultivaluedMap<String, String> pathParameters,
            final String queueNamePathParameter) {

        if(!pathParameters.containsKey(queueNamePathParameter)) {
            return java.util.Optional.empty();
        }

        final String pathQueueName = pathParameters.getFirst(queueNamePathParameter);

        if(Strings.isNullOrEmpty(pathQueueName)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(QueueName.valueOf(pathQueueName));
    }

    private RequestParameters parseHeaderRequestParameters(final ContainerRequestContext requestContext, final AccountName accountName) {
        final String header = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!Strings.isNullOrEmpty(header)) {
            final Splitter splitter = Splitter.on(' ').limit(2).trimResults();
            final List<String> components = splitter.splitToList(header);

            if (components.size() == 2) {
                final String authScheme = components.get(0);

                // first check if the request is using a raw key scheme
                if (keyAuthPrefix.equalsIgnoreCase(authScheme)) {
                    return AccountKeyParameters.builder()
                                               .accountName(accountName)
                                               .authKey(components.get(1))
                                               .build();
                }

                // then check if the request is using a "signed" (or what ever we're configured to expect via prefix) request scheme,
                if (prefix.equalsIgnoreCase(authScheme)) {
                    final DateTime requestTime = parseRequestTimeHeader(requestContext.getHeaders());

                    return SignedRequestParameters.builder()
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

        if(Strings.isNullOrEmpty(requestTimeHeaderValue)) {
            return new DateTime(0); // min value
        }

        return SignatureGenerator.parseDateTime(requestTimeHeaderValue);
    }

    private SignedUrlParameters parseSignedUrlParameters(
            final ContainerRequestContext requestContext,
            final AccountName accountName,
            final java.util.Optional<QueueName> queueName) {

        final MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();

        final String sig = queryParameters.getFirst(SignedUrlParameterNames.Signature.getParameterName());

        if (Strings.isNullOrEmpty(sig)) {
            return null;
        }

        final EnumSet<AuthorizationLevel> authorizationLevels = parseAuthorizationLevel(queryParameters);

        final java.util.Optional<DateTime> startTime = parseTimeParam(queryParameters, SignedUrlParameterNames.StartTime);
        final java.util.Optional<DateTime> endTime = parseTimeParam(queryParameters, SignedUrlParameterNames.EndTime);

        return SignedUrlParameters.builder()
                                  .accountName(accountName)
                                  .authorizationLevels(authorizationLevels)
                                  .querySignature(sig)
                                  .endDateTime(endTime)
                                  .startDateTime(startTime)
                                  .queueName(queueName)
                                  .build();

    }

    private java.util.Optional<DateTime> parseTimeParam(
            final MultivaluedMap<String, String> queryParameters,
            final SignedUrlParameterNames parameter) {
        final String timeParam = queryParameters.getFirst(parameter.getParameterName());

        if (Strings.isNullOrEmpty(timeParam)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(SignatureGenerator.parseDateTime(timeParam));
    }

    private EnumSet<AuthorizationLevel> parseAuthorizationLevel(final MultivaluedMap<String, String> queryParameters) {
        String authParam = queryParameters.getFirst(SignedUrlParameterNames.AuthorizationLevels.getParameterName());

        return AuthorizationLevel.parse(authParam);
    }

    private boolean setPrincipal(final ContainerRequestContext requestContext, final AuthorizedRequestCredentials credentials) {
        try {

            final Optional<TPrincipal> optionalPrincipal = authenticator.authenticate(credentials);

            if (optionalPrincipal.isPresent()) {

                final TPrincipal principal = optionalPrincipal.get();

                final SecurityContext previousSecurityContext = requestContext.getSecurityContext();

                final AccountSecurityContext<TPrincipal> newAccountSecurityContext =
                        new AccountSecurityContext<>(principal, authorizer, previousSecurityContext);

                requestContext.setSecurityContext(newAccountSecurityContext);

                return true;
            }
        }
        catch (AuthenticationException e) {
            logger.warn("Error authenticating credentials", e);
            throw new InternalServerErrorException();
        }

        return false;
    }

    public static class Builder<TPrincipal extends Principal>
            extends AuthFilterBuilder<AuthorizedRequestCredentials, TPrincipal, SignedRequestAuthenticationFilter<TPrincipal>> {

        public Builder() {
            setPrefix("Signed");
            setRealm("cassieq");
        }

        @Override
        protected SignedRequestAuthenticationFilter<TPrincipal> newInstance() {
            final SignedRequestAuthenticationFilter<TPrincipal> filter = new SignedRequestAuthenticationFilter<>(
                    Strings.isNullOrEmpty(accountNamePathParameter) ? "accountName" : accountNamePathParameter,
                    Strings.isNullOrEmpty(queueNamePathParameter) ? "queueName" : queueNamePathParameter,
                    Strings.isNullOrEmpty(keyAuthPrefix) ? "Key" : keyAuthPrefix);

            return filter;
        }

        public SignedRequestAuthenticationFilter<TPrincipal> build() {
            return buildAuthFilter();
        }
    }
}
