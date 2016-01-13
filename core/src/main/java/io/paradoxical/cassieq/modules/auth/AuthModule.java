package io.paradoxical.cassieq.modules.auth;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import io.dropwizard.auth.Authenticator;
import io.paradoxical.cassieq.auth.AccountPrincipal;
import io.paradoxical.cassieq.auth.AuthToken;
import io.paradoxical.cassieq.auth.SignedRequestAuthenticator;
import io.paradoxical.cassieq.model.time.Clock;
import io.paradoxical.cassieq.model.time.JodaClock;

public class AuthModule extends AbstractModule {
    @Override protected void configure() {
//        bind(new TypeLiteral<Authenticator<AuthToken, AccountPrincipal>>(){})
//                .to(SignedRequestAuthenticator.class)
//                .in(LazySingletonScope.get());
    }

    @Provides
    @LazySingleton
    public Authenticator<AuthToken, AccountPrincipal> getAuthenticator() {
        return new SignedRequestAuthenticator();
    }
}
