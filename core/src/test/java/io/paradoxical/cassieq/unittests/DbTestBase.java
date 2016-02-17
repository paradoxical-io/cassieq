package io.paradoxical.cassieq.unittests;

import com.datastax.driver.core.Session;
import com.godaddy.logging.Logger;
import com.google.inject.Injector;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.dataAccess.CqlDb;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class DbTestBase extends TestBase {
    public static Session session;

    private static final Logger logger = getLogger(DbTestBase.class);

    static{
        synchronized (lock) {
            if (session == null) {
                try {
                    session = CqlDb.create();
                }
                catch (Exception e) {
                    logger.error(e, "Error");

                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    protected Injector getDefaultInjector() {
        return getDefaultInjector(new ServiceConfiguration());
    }

    protected Injector getDefaultInjector(ServiceConfiguration configuration) {
        return getDefaultInjector(configuration, session);
    }

    protected Injector getDefaultInjector(ServiceConfiguration configuration, Session session) {
        return getDefaultInjector(configuration, new InMemorySessionProvider(session));
    }

}
