package io.paradoxical.cassieq.auth;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

public class SignedRequestAuthenticator implements Authenticator<SignedRequestCredentials, AccountPrincipal> {

    @Override
    public Optional<AccountPrincipal> authenticate(final SignedRequestCredentials credentials) throws AuthenticationException {
        return Optional.absent();
    }
}
