package io.paradoxical.cassieq.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

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
public class SignedRequestAuthFilter<P extends Principal> extends AuthFilter<SignedRequestCredentials, P> {
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

                final String accountName = pathParameters.getFirst(accountNamePathParameter);

                final String signature = parseSignature(requestContext);

                final String key = parseAuthKey(requestContext);

                final EnumSet<AuthorizationLevel> authorizationLevels = parseAuthorizationLevel(requestContext);


                final SignedRequestCredentials credentials =
                        SignedRequestCredentials.builder()
                                                .signature(signature)
                                                .requestPath(requestContext.getUriInfo().getPath())
                                                .requestMethod(requestContext.getMethod())
                                                .accountName(AccountName.valueOf(accountName))
                                                .authorizationLevels(authorizationLevels)
                                                .authKey(key)
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

    private String parseAuthKey(final ContainerRequestContext requestContext) {
        final String header = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        final String keyValue = parseAuthHeader(header, keyAuthPrefix);

        if (keyValue != null) {
            return keyValue;
        }

        return StringUtils.EMPTY;

    }

    private String parseAuthHeader(final String header, final String expectedPrefix) {
        if (!Strings.isNullOrEmpty(header)) {
            final Splitter splitter = Splitter.on(' ').limit(2).trimResults();
            final List<String> components = splitter.splitToList(header);

            if (components.size() == 2 && expectedPrefix.equalsIgnoreCase(components.get(0))) {
                return Strings.nullToEmpty(components.get(1));
            }
        }
        return null;
    }

    private EnumSet<AuthorizationLevel> parseAuthorizationLevel(final ContainerRequestContext requestContext) {
        final String header = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!Strings.isNullOrEmpty(header)) {
            return AuthorizationLevel.All;
        }

        String authParam = requestContext.getUriInfo().getQueryParameters().getFirst("auth");

        if (!Strings.isNullOrEmpty(authParam)) {
            return AuthorizationLevel.parse(authParam);
        }

        return AuthorizationLevel.emptyPermissions();
    }


    private String parseSignature(final ContainerRequestContext requestContext) {
        final String header = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        String headerSignature = parseAuthHeader(header, prefix);

        if (!Strings.isNullOrEmpty(headerSignature)) {
            return headerSignature;
        }

        final String sig = requestContext.getUriInfo().getQueryParameters().getFirst("sig");

        return Strings.nullToEmpty(sig);
    }

    private boolean setPrincipal(final ContainerRequestContext requestContext, final SignedRequestCredentials credentials) {
        try {
            final Optional<P> optionalPrincipal = authenticator.authenticate(credentials);
            if (optionalPrincipal.isPresent()) {

                final P principal = optionalPrincipal.get();

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

    public static class Builder<P extends Principal> extends AuthFilterBuilder<SignedRequestCredentials, P, SignedRequestAuthFilter<P>> {

        @Override
        protected SignedRequestAuthFilter<P> newInstance() {
            return new SignedRequestAuthFilter<>(
                    Strings.isNullOrEmpty(accountNamePathParameter) ? "accountName" : accountNamePathParameter,
                    Strings.isNullOrEmpty(keyAuthPrefix) ? "Key" : keyAuthPrefix);
        }

        public SignedRequestAuthFilter<P> build() {
            return buildAuthFilter();
        }
    }
}
