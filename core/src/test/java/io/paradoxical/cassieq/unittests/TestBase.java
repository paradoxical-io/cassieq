package io.paradoxical.cassieq.unittests;

import ch.qos.logback.classic.Level;
import com.datastax.driver.core.Session;
import com.godaddy.logging.Logger;
import com.google.inject.Injector;
import com.netflix.governator.Governator;
import io.dropwizard.logging.BootstrapLogging;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.configurations.LogMapping;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.modules.DefaultApplicationModules;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.modules.MockEnvironmentModule;
import io.paradoxical.cassieq.unittests.modules.TestClockModule;
import io.paradoxical.cassieq.unittests.time.TestClock;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;
import io.paradoxical.common.test.guice.ModuleUtils;
import io.paradoxical.common.test.guice.OverridableModule;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.junit.After;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

public class TestBase {
    private static final Logger logger = getLogger(TestBase.class);

    public static Session session;

    private static final Object lock = new Object();

    static {
        final String environmentLogLevel = System.getenv("LOG_LEVEL");

        BootstrapLogging.bootstrap(environmentLogLevel != null ? Level.toLevel(environmentLogLevel) : Level.ERROR);

        LogMapping.register();

        String[] disableLogging = new String[]{ "uk.co.jemos.podam",
                                                "com.datastax",
                                                "org.cassandraunit",
                                                "io.netty",
                                                "org.glassfish",
                                                "org.apache"
        };

        Arrays.stream(disableLogging).forEach(i -> {
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(i)).setLevel(Level.OFF);
        });

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

    @Getter(AccessLevel.PROTECTED)
    private final TestClock testClock = new TestClock();

    public TestBase() {

    }

    protected Injector getDefaultInjector(ServiceConfiguration configuration) {
        return getDefaultInjector(configuration, session);
    }

    protected Injector getDefaultInjector(ServiceConfiguration configuration, Session session) {
        return getDefaultInjector(configuration, new InMemorySessionProvider(session));
    }

    protected Injector getDefaultInjector() {
        return getDefaultInjector(new ServiceConfiguration());
    }

    protected Injector getDefaultInjector(OverridableModule... modules) {
        return getDefaultInjector(new ServiceConfiguration(), modules);
    }

    protected Injector getDefaultInjector(final ServiceConfiguration serviceConfiguration, OverridableModule... modules) {
        return getDefaultInjectorRaw(ListUtils.union(Arrays.asList(modules),
                                                     Arrays.asList(new MockEnvironmentModule(serviceConfiguration),
                                                                   new TestClockModule(testClock))));
    }

    private Injector getDefaultInjectorRaw(final List<OverridableModule> overridableModules) {
        return Governator.createInjector(
                ModuleUtils.mergeModules(DefaultApplicationModules.getModules(),
                                         overridableModules));
    }

    protected QueueDefinition setupQueue(QueueName queueName) {
        return setupQueue(queueName, 20, getDefaultInjector());
    }

    protected QueueDefinition setupQueue(QueueName queueName, Integer bucketSize) {
        return setupQueue(queueName, bucketSize, getDefaultInjector());
    }

    protected QueueDefinition setupQueue(QueueName queueName, Integer bucketSize, Injector injector) {
        final QueueDefinition queueDefinition = QueueDefinition.builder()
                                                               .queueName(queueName)
                                                               .bucketSize(BucketSize.valueOf(bucketSize))
                                                               .repairWorkerPollFrequencySeconds(1)
                                                               .repairWorkerTombstonedBucketTimeoutSeconds(3)
                                                               .build();

        createQueue(queueDefinition, injector);

        return queueDefinition;
    }

    private void createQueue(final QueueDefinition queueDefinition, final Injector injector) {
        final QueueRepository queueRepository = injector.getInstance(QueueRepository.class);

        queueRepository.createQueue(queueDefinition);

        assertThat(queueRepository.getQueueUnsafe(queueDefinition.getQueueName()).isPresent()).isTrue();
    }

    protected void createQueue(final QueueDefinition queueDefinition) {
        createQueue(queueDefinition, getDefaultInjector());
    }
}
