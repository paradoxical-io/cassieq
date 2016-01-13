package io.paradoxical.cassieq.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.auth.Auth;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toList;

@Priority(Priorities.AUTHENTICATION)
@Builder(builderClassName = "Builder")
public class SignedRequestAuthFilter<P extends Principal> extends AuthFilter<AuthToken, P> {
    private static final Logger logger = getLogger(SignedRequestAuthFilter.class);

    private final String accountNamePathParameter;

    public SignedRequestAuthFilter(String accountNamePathParameter) {
        this.accountNamePathParameter = accountNamePathParameter;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        try {
            final MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
            if (!pathParameters.containsKey(accountNamePathParameter)) {

                final String accountName = pathParameters.getFirst(accountNamePathParameter);

                final String signature = parseSignature(requestContext);

                final EnumSet<AuthorizationLevel> authorizationLevels = parseAuthorizationLevel(requestContext);

                final AuthToken credentials =
                        AuthToken.builder()
                                 .signature(signature)
                                 .requestPath(requestContext.getUriInfo().getPath())
                                 .requestMethod(requestContext.getMethod())
                                 .accountName(AccountName.valueOf(accountName))
                                 .authorizationLevels(authorizationLevels)
                                 .build();

                if (setPrincipal(requestContext, credentials)) {
                    return;
                }
            }
        }
        catch (IllegalArgumentException e) {
            logger.warn("Error decoding credentials", e);
        }

        throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
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

        return EnumSet.noneOf(AuthorizationLevel.class);
    }


    private String parseSignature(final ContainerRequestContext requestContext) {
        final String header = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!Strings.isNullOrEmpty(header)) {
            final Splitter splitter = Splitter.on(' ').limit(2).trimResults();
            final List<String> components = splitter.splitToList(header);

            if (components.size() == 2 && prefix.equalsIgnoreCase(components.get(0))) {

                return components.get(1);
            }
        }

        return requestContext.getUriInfo().getQueryParameters().getFirst("sig");
    }

    private boolean setPrincipal(final ContainerRequestContext requestContext, final AuthToken credentials) {
        try {
            final Optional<P> principal = authenticator.authenticate(credentials);
            if (principal.isPresent()) {
                requestContext.setSecurityContext(new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return principal.get();
                    }

                    @Override
                    public boolean isUserInRole(String role) {
                        return authorizer.authorize(principal.get(), role);
                    }

                    @Override
                    public boolean isSecure() {
                        return requestContext.getSecurityContext().isSecure();
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "Signed";
                    }
                });
                return true;
            }
        }
        catch (AuthenticationException e) {
            logger.warn("Error authenticating credentials", e);
            throw new InternalServerErrorException();
        }
        return false;
    }

    public static class Builder<P extends Principal> extends AuthFilterBuilder<AuthToken, P, SignedRequestAuthFilter<P>> {

        @Override
        protected SignedRequestAuthFilter<P> newInstance() {
            return new SignedRequestAuthFilter<>(
                    Strings.isNullOrEmpty(accountNamePathParameter) ? "accountName" : accountNamePathParameter);
        }

        public SignedRequestAuthFilter<P> build()
        {
            return buildAuthFilter();
        }
    }
}
