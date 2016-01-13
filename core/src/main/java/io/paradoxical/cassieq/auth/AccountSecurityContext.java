package io.paradoxical.cassieq.auth;

import io.dropwizard.auth.Authorizer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class AccountSecurityContext<P extends Principal>  implements SecurityContext {
    private final P principal;
    private final Authorizer<P> authorizer;
    private final ContainerRequestContext requestContext;

    public AccountSecurityContext(
            final P principal,
            Authorizer<P> authorizer,
            ContainerRequestContext requestContext) {

        this.principal = principal;
        this.authorizer = authorizer;
        this.requestContext = requestContext;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return authorizer.authorize(principal, role);
    }

    @Override
    public boolean isSecure() {
        return requestContext.getSecurityContext().isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return "Signed";
    }
}
