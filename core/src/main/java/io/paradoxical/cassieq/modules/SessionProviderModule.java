package io.paradoxical.cassieq.modules;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.dropwizard.setup.Environment;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.dataAccess.SessionProxy;

public class SessionProviderModule extends AbstractModule {

    private static Session session;

    @Override
    protected void configure() {
    }

    @Provides
    @LazySingleton
    public Session getSession(final ServiceConfiguration config, final Environment env) {
        if (session == null) {
            Cluster cluster = config.getCassandra().build(env);

            final String keyspace = config.getCassandra().getKeyspace();

            session = keyspace != null ? cluster.connect(keyspace) : cluster.connect();
        }

        if (config.getCassandra().getCasConfig().getEnabled()) {
            return new SessionProxy(session, config.getCassandra());
        }

        return session;
    }
}