package io.paradoxical.cassieq.discoverable.auth;

import com.godaddy.logging.Logger;
import io.dropwizard.auth.Authenticator;
import io.paradoxical.cassieq.discoverable.resources.api.v1.AuthLevelRequired;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.AuthorizedRequestCredentials;
import org.glassfish.jersey.server.model.AnnotatedMethod;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;

import static com.godaddy.logging.LoggerFactory.getLogger;

@Provider
public class AuthLevelDynamicFeature implements DynamicFeature {
    private static final Logger logger = getLogger(AuthLevelDynamicFeature.class);

    private final Authenticator<AuthorizedRequestCredentials, AccountPrincipal> authenticator;

    @Inject
    public AuthLevelDynamicFeature(Authenticator<AuthorizedRequestCredentials, AccountPrincipal> authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
        final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

        /**
         * Register a custom filter for each method based on what the filter asked its auth level to be
         */
        if (am.isAnnotationPresent(AuthLevelRequired.class)) {
            logger.with("method", resourceInfo.getResourceMethod().getName())
                  .with("resource", resourceInfo.getResourceClass().getSimpleName())
                  .info("Registering auth filter");

            final EnumSet<AuthorizationLevel> requiredLevels = EnumSet.copyOf(Arrays.asList(am.getAnnotation(AuthLevelRequired.class).levels()));

            context.register(getAuthFilter(requiredLevels));

            context.register(new AuthorizationFilter());
        }
    }

    public SignedRequestAuthenticationFilter<AccountPrincipal> getAuthFilter(EnumSet<AuthorizationLevel> allowedLevels) {
        return SignedRequestAuthenticationFilter.<AccountPrincipal>builder()
                .accountNamePathParameter("accountName")
                .setAuthenticator(authenticator)
                .setPrefix("Signed")
                .setAuthorizer((principal, _ignore) -> {
                    final EnumSet<AuthorizationLevel> claimedLevels = principal.getAuthorizationLevels();

                    return allowedLevels.stream().anyMatch(claimedLevels::contains);
                })
                .setUnauthorizedHandler((prefix, realm) -> Response.status(Response.Status.UNAUTHORIZED).build())
                .buildAuthFilter();
    }

    @Priority(Priorities.AUTHORIZATION) // authorization filter - should go after any authentication filters
    private static class AuthorizationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            /**
             * Authenticated but not authorized
             */
            if (!requestContext.getSecurityContext().isUserInRole(null)) {
                throw new ForbiddenException();
            }
        }
    }
}
