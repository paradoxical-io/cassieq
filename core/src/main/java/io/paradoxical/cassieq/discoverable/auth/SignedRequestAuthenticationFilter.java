package io.paradoxical.cassieq.discoverable.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.paradoxical.cassieq.discoverable.auth.parsers.HeaderAuthParametersParser;
import io.paradoxical.cassieq.discoverable.auth.parsers.SignedUrlAuthParametersParser;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;

import javax.annotation.Priority;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

import static com.godaddy.logging.LoggerFactory.getLogger;


@AccountAuth
@Priority(Priorities.AUTHENTICATION)
@Builder(builderClassName = "Builder")
public class SignedRequestAuthenticationFilter<TPrincipal extends Principal> extends AuthFilter<AuthorizedRequestCredentials, TPrincipal> {
    private static final Logger logger = getLogger(SignedRequestAuthenticationFilter.class);

    private final String accountNamePathParameter;

    private final String queueNamePathParameter;

    private final String keyAuthPrefix;

    private SignedRequestAuthenticationFilter(
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

                final java.util.Optional<QueueName> requestedQueueName = tryGetRequestedQueueName(pathParameters, queueNamePathParameter);

                final java.util.Optional<RequestAuthParameters> headerRequestAuthParameters =
                        new HeaderAuthParametersParser(keyAuthPrefix, prefix)
                                .tryParse(requestContext, accountName);

                final java.util.Optional<SignedUrlAuthParameters> signedUrlAuthParameters =
                        new SignedUrlAuthParametersParser()
                                .tryParse(requestContext, accountName);

                // the request params we'll use to auth, with priority from header (full access) vs query param
                final RequestAuthParameters requestAuthParameters =
                        headerRequestAuthParameters
                                .orElseGet(() -> signedUrlAuthParameters.orElseThrow(this::buildUnauthorizedException));


                final AuthorizedRequestCredentials credentials =
                        AuthorizedRequestCredentials.builder()
                                                    .requestAuthParameters(requestAuthParameters)
                                                    .queueName(requestedQueueName)
                                                    .accountName(accountName)
                                                    .build();

                if (trySetPrincipal(requestContext, credentials)) {
                    return;
                }
            }
            else {
                // if no account in the request path, no auth is required
                return;
            }
        }
        catch (IllegalArgumentException e) {
            logger.warn("Error decoding credentials", e);
        }

        throw buildUnauthorizedException();
    }

    private WebApplicationException buildUnauthorizedException() {
        return new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
    }

    private java.util.Optional<QueueName> tryGetRequestedQueueName(
            final MultivaluedMap<String, String> pathParameters,
            final String queueNamePathParameter) {

        if (!pathParameters.containsKey(queueNamePathParameter)) {
            return java.util.Optional.empty();
        }

        final String pathQueueName = pathParameters.getFirst(queueNamePathParameter);

        if (Strings.isNullOrEmpty(pathQueueName)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(QueueName.valueOf(pathQueueName));
    }

    private boolean trySetPrincipal(final ContainerRequestContext requestContext, final AuthorizedRequestCredentials credentials) {
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
