package io.paradoxical.cassieq.modules.auth;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.dropwizard.auth.Authenticator;
import io.paradoxical.cassieq.auth.AccountPrincipal;
import io.paradoxical.cassieq.model.auth.AuthorizedRequestCredentials;
import io.paradoxical.cassieq.auth.SignedRequestAuthenticator;
import io.paradoxical.cassieq.factories.DataContextFactory;

public class AuthModule extends AbstractModule {
    @Override protected void configure() {
    }

    @Provides
    @LazySingleton
    public Authenticator<AuthorizedRequestCredentials, AccountPrincipal> getAuthenticator(DataContextFactory dataContextFactory) {
        return new SignedRequestAuthenticator(dataContextFactory);
    }
}