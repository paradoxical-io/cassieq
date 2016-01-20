package io.paradoxical.cassieq.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AccountKeyParameters;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.AuthorizedRequestCredentials;
import io.paradoxical.cassieq.model.auth.RequestParameters;
import io.paradoxical.cassieq.model.auth.SignedRequestParameters;
import io.paradoxical.cassieq.model.auth.SignedUrlParameters;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import javax.annotation.Priority;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.security.Principal;
import java.util.EnumSet;
import java.util.List;

import static com.godaddy.logging.LoggerFactory.getLogger;

@Priority(Priorities.AUTHENTICATION)
@Builder(builderClassName = "Builder")
public class SignedRequestAuthFilter<TPrincipal extends Principal> extends AuthFilter<AuthorizedRequestCredentials, TPrincipal> {
    private static final Logger logger = getLogger(SignedRequestAuthFilter.class);

    private final String accountNamePathParameter;
    private final String keyAuthPrefix;

    public SignedRequestAuthFilter(String accountNamePathParameter, final String keyAuthPrefix) {
        this.accountNamePathParameter = accountNamePathParameter;
        this.keyAuthPrefix = keyAuthPrefix;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        try {

            final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
            if (pathParameters.containsKey(accountNamePathParameter)) {

                final AccountName accountName = AccountName.valueOf(pathParameters.getFirst(accountNamePathParameter));

                final RequestParameters headerRequestParameters = parseHeaderRequestParameters(requestContext, accountName);
                final SignedUrlParameters signedUrlParameters = parseSignedUrlParameters(requestContext, accountName);

                final AuthorizedRequestCredentials credentials =
                        AuthorizedRequestCredentials.builder()
                                                    .requestParameters(MoreObjects.firstNonNull(headerRequestParameters, signedUrlParameters))
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
                    return SignedRequestParameters.builder()
                                                  .accountName(accountName)
                                                  .requestMethod(requestContext.getMethod())
                                                  .providedSignature(components.get(1))
                                                  .requestPath(requestContext.getUriInfo().getPath())
                                                  .build();
                }
            }
        }

        return null;
    }

    private SignedUrlParameters parseSignedUrlParameters(final ContainerRequestContext requestContext, final AccountName accountName) {
        final String sig = requestContext.getUriInfo().getQueryParameters().getFirst(SignedUrlParameterNames.Signature.getParameterName());

        if (Strings.isNullOrEmpty(sig)) {
            return null;
        }

        final EnumSet<AuthorizationLevel> authorizationLevels = parseAuthorizationLevel(requestContext);

        return SignedUrlParameters.builder()
                                  .accountName(accountName)
                                  .authorizationLevels(authorizationLevels)
                                  .querySignature(sig)
                                  .build();

    }

    private EnumSet<AuthorizationLevel> parseAuthorizationLevel(final ContainerRequestContext requestContext) {
        String authParam = requestContext.getUriInfo().getQueryParameters().getFirst(SignedUrlParameterNames.AuthorizationLevels.getParameterName());

        return AuthorizationLevel.parse(authParam);
    }


    private boolean setPrincipal(final ContainerRequestContext requestContext, final AuthorizedRequestCredentials credentials) {
        try {
            final Optional<TPrincipal> optionalPrincipal = authenticator.authenticate(credentials);
            if (optionalPrincipal.isPresent()) {

                final TPrincipal principal = optionalPrincipal.get();

                requestContext.setSecurityContext(new AccountSecurityContext<>(principal, authorizer, requestContext));

                return true;
            }
        }
        catch (AuthenticationException e) {
            logger.warn("Error authenticating credentials", e);
            throw new InternalServerErrorException();
        }

        return false;
    }

    public static class Builder<TPrincipal extends Principal> extends AuthFilterBuilder<AuthorizedRequestCredentials, TPrincipal, SignedRequestAuthFilter<TPrincipal>> {

        public Builder() {
            setPrefix("Signed");
            setRealm("cassieq");
        }

        @Override
        protected SignedRequestAuthFilter<TPrincipal> newInstance() {
            final SignedRequestAuthFilter<TPrincipal> filter = new SignedRequestAuthFilter<>(
                    Strings.isNullOrEmpty(accountNamePathParameter) ? "accountName" : accountNamePathParameter,
                    Strings.isNullOrEmpty(keyAuthPrefix) ? "Key" : keyAuthPrefix);

            return filter;
        }

        public SignedRequestAuthFilter<TPrincipal> build() {
            return buildAuthFilter();
        }
    }
}
