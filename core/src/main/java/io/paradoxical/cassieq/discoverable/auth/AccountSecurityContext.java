package io.paradoxical.cassieq.discoverable.auth;

import io.dropwizard.auth.Authorizer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class AccountSecurityContext<P extends Principal>  implements SecurityContext {
    private final P principal;
    private final Authorizer<P> authorizer;
    private final SecurityContext previousContext;

    public AccountSecurityContext(
            final P principal,
            final Authorizer<P> authorizer,
            final SecurityContext previousContext) {

        this.principal = principal;
        this.authorizer = authorizer;
        this.previousContext = previousContext;
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
        return previousContext.isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return "Signed";
    }
}
