package io.paradoxical.cassieq.unittests.modules;

import com.datastax.driver.core.Session;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.paradoxical.cassieq.configurations.cassandra.CassandraConfiguration;
import io.paradoxical.cassieq.dataAccess.SessionProxy;
import io.paradoxical.cassieq.modules.SessionProviderModule;
import io.paradoxical.common.test.guice.OverridableModule;

public class InMemorySessionProvider extends OverridableModule {
    private final Session session;

    public InMemorySessionProvider(Session session) {
        this.session = session;
    }

    @Override
    public Class<? extends Module> getOverridesModule() {
        return SessionProviderModule.class;
    }

    @Override
    protected void configure() {

    }

    @Provides
    @LazySingleton
    public Session session(CassandraConfiguration configuration) {
        return new SessionProxy(session, configuration);
    }
}
