package io.paradoxical.cassieq.auth;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

public class SignedRequestAuthenticator implements Authenticator<AuthToken, AccountPrincipal> {

    @Override
    public Optional<AccountPrincipal> authenticate(final AuthToken credentials) throws AuthenticationException {
        return Optional.absent();
    }
}
