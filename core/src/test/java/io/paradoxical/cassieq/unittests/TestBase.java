package io.paradoxical.cassieq.unittests;

import ch.qos.logback.classic.Level;
import com.datastax.driver.core.Session;
import com.godaddy.logging.Logger;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.logging.BootstrapLogging;
import io.dropwizard.logging.LoggingFactory;
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
import io.paradoxical.common.test.guice.ModuleUtils;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class TestBase {
    private static final Logger logger = getLogger(TestBase.class);

    public static final Session session;

    static {
        try {
            session = CqlDb.create();
        }
        catch (Exception e) {
            logger.error(e, "Error");

            throw new RuntimeException(e);
        }
    }

    @Getter(AccessLevel.PROTECTED)
    private final TestClock testClock = new TestClock();

    public TestBase() {
        final String environmentLogLevel = System.getenv("LOG_LEVEL");

        BootstrapLogging.bootstrap(environmentLogLevel != null ? Level.toLevel(environmentLogLevel) : Level.ERROR);

        LogMapping.register();

        String[] disableLogging = new String[]{ "uk.co.jemos.podam.api.PodamFactoryImpl",
                                                "uk.co.jemos.podam.common.BeanValidationStrategy",
                                                "org.apache.cassandra.service.CassandraDaemon",
                                                "org.apache.cassandra.service.CacheService",
                                                "org.apache.cassandra.db.Memtable",
                                                "org.apache.cassandra.db.ColumnFamilyStore",
                                                "org.apache.cassandra.config.DatabaseDescriptor",
                                                "org.apache.cassandra.db.compaction.CompactionTask",
                                                "org.apache.cassandra.db.DefsTables",
                                                "org.apache.cassandra.service.MigrationManager",
                                                "org.apache.cassandra.config.YamlConfigurationLoader",
                                                "org.apache.cassandra.service.StorageService"
        };

        Arrays.stream(disableLogging).forEach(i -> {
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(i)).setLevel(Level.OFF);
        });
    }

    protected Injector getDefaultInjector(ServiceConfiguration configuration) {
        return Guice.createInjector(
                ModuleUtils.mergeModules(DefaultApplicationModules.getModules(),
                                         new InMemorySessionProvider(session),
                                         new MockEnvironmentModule(configuration),
                                         new TestClockModule(testClock)));
    }

    protected Injector getDefaultInjector() {
        return getDefaultInjector(new ServiceConfiguration());
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
                                                               .build();

        createQueue(queueDefinition, injector);
        return queueDefinition;
    }

    private void createQueue(final QueueDefinition queueDefinition, final Injector injector) {
        final QueueRepository queueRepository = injector.getInstance(QueueRepository.class);

        queueRepository.createQueue(queueDefinition);

        queueRepository.getQueue(queueDefinition.getQueueName()).get();
    }

    protected void createQueue(final QueueDefinition queueDefinition) {
        createQueue(queueDefinition, getDefaultInjector());
    }
}
