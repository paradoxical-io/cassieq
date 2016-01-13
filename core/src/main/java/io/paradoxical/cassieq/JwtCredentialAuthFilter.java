package io.paradoxical.cassieq;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTVerifyException;
import com.godaddy.logging.Logger;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentials;
import lombok.Builder;

import javax.annotation.Priority;
import javax.annotation.concurrent.Immutable;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SignatureException;
import java.util.List;
import java.util.Map;

import static com.godaddy.logging.LoggerFactory.getLogger;

@Priority(Priorities.AUTHENTICATION)
@Builder(builderClassName = "Builder")
public class JwtCredentialAuthFilter<P extends Principal> extends AuthFilter<JwtToken, P> {
    private static final Logger logger = getLogger(JwtCredentialAuthFilter.class);

    private final JWTVerifier jwtVerifier;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final String header = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        try {
            if (header != null) {
                final int space = header.indexOf(' ');


                final Splitter splitter = Splitter.on(' ').limit(2).trimResults();
                final List<String> components = splitter.splitToList(header);

                if(components.size() == 2 && prefix.equalsIgnoreCase(components.get(0))) {

                    try {
                        final Map<String, Object> claims = jwtVerifier.verify(components.get(1));

                        final JwtToken credentials = new JwtToken(makeImmutable(claims));
                        if (setPrincipal(requestContext, credentials)) {
                            return;
                        }

                    }
                    catch (NoSuchAlgorithmException e) {
                        logger.error(e, "Error");
                    }
                    catch (InvalidKeyException e) {
                        logger.error(e, "Error");
                    }
                    catch (SignatureException e) {
                        logger.error(e, "Error");
                    }
                    catch (JWTVerifyException e) {
                        logger.error(e, "Error");
                    }


                }
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Error decoding credentials", e);
        }

        throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
    }

    private ImmutableMap<String, Object> makeImmutable(final Map<String, Object> claims) {

        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

        claims.forEach((key, value) -> {
            if(value instanceof Map){
                builder.put(key, makeImmutable((Map<String, Object>)value));
            }
            else{
                builder.put(key, value);
            }
        });

        return builder.build();
    }

    private boolean setPrincipal(final ContainerRequestContext requestContext, final JwtToken credentials) {
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
                        return SecurityContext.BASIC_AUTH;
                    }
                });
                return true;
            }
        } catch (AuthenticationException e) {
            logger.warn("Error authenticating credentials", e);
            throw new InternalServerErrorException();
        }
        return false;
    }

    public static class Builder<P extends Principal> extends
                                                     AuthFilterBuilder<JwtToken, P, JwtCredentialAuthFilter<P>> {

        @Override
        protected JwtCredentialAuthFilter<P> newInstance() {
            return new JwtCredentialAuthFilter<>(jwtVerifier);
        }
    }
}
