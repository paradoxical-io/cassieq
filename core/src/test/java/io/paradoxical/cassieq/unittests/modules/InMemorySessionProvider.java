package io.paradoxical.cassieq.unittests.modules;

import com.datastax.driver.core.Session;
import io.paradoxical.cassieq.modules.SessionProviderModule;
import io.paradoxical.common.test.guice.OverridableModule;
import com.google.inject.Module;

public class InMemorySessionProvider extends OverridableModule {
    private final Session session;

    public InMemorySessionProvider(Session session) {
        this.session = session;
    }

    @Override public Class<? extends Module> getOverridesModule() {
        return SessionProviderModule.class;
    }

    @Override protected void configure() {
        bind(Session.class).toInstance(session);
    }
}
