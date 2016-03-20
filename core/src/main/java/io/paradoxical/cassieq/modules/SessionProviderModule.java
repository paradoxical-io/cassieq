package io.paradoxical.cassieq.modules;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.godaddy.logging.Logger;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.dropwizard.setup.Environment;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.dataAccess.SessionProxy;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class SessionProviderModule extends AbstractModule {
    private static final Logger logger = getLogger(SessionProviderModule.class);

    private Session session;

    @Override
    protected void configure() {
    }

    @Provides
    @LazySingleton
    public Session getSession(final ServiceConfiguration config, final Environment env) {
        try {
            if (session == null) {
                Cluster cluster = config.getCassandra().build(env);

                final String keyspace = config.getCassandra().getKeyspace();

                try {
                    session = keyspace != null ? cluster.connect(keyspace) : cluster.connect();
                } catch (InvalidQueryException ex) {
                    logger.error(String.format("Unable to start cassieq! Make sure to run 'setup-db' first: %s", ex.getLocalizedMessage()));
                    System.exit(1);
                }
            }

            if (config.getCassandra().getCasConfig().getEnabled()) {
                session = new SessionProxy(session, config.getCassandra());
            }
        }
        catch(Throwable ex){
            logger.error("An unknown error occurred. Check your cassandra configuration: {}", ex.getLocalizedMessage());

            System.exit(1);
        }

        return session;
    }
}