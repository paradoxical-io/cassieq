package io.paradoxical.cassieq.modules.auth;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.dropwizard.auth.Authenticator;
import io.paradoxical.cassieq.auth.AccountPrincipal;
import io.paradoxical.cassieq.auth.SignedRequestCredentials;
import io.paradoxical.cassieq.auth.SignedRequestAuthenticator;

public class AuthModule extends AbstractModule {
    @Override protected void configure() {
//        bind(new TypeLiteral<Authenticator<SignedRequestCredentials, AccountPrincipal>>(){})
//                .to(SignedRequestAuthenticator.class)
//                .in(LazySingletonScope.get());
    }

    @Provides
    @LazySingleton
    public Authenticator<SignedRequestCredentials, AccountPrincipal> getAuthenticator() {
        return new SignedRequestAuthenticator();
    }
}
