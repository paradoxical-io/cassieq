package io.paradoxical.cassieq.unittests;

import ch.qos.logback.classic.Level;
import com.godaddy.logging.Logger;
import com.google.inject.Injector;
import com.netflix.governator.Governator;
import io.dropwizard.logging.BootstrapLogging;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.configurations.LogMapping;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.modules.DefaultApplicationModules;
import io.paradoxical.cassieq.unittests.modules.HazelcastTestModule;
import io.paradoxical.cassieq.unittests.modules.MockEnvironmentModule;
import io.paradoxical.cassieq.unittests.modules.TestClockModule;
import io.paradoxical.cassieq.unittests.time.TestClock;
import io.paradoxical.common.test.guice.ModuleUtils;
import io.paradoxical.common.test.guice.OverridableModule;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

public class TestBase {
    private static final Logger logger = getLogger(TestBase.class);

    protected static final Object lock = new Object();

    public static final AccountName testAccountName = AccountName.valueOf("test");

    protected static final PodamFactory fixture = new PodamFactoryImpl();

    private HazelcastTestModule hazelCastModule;

    static {
        final String environmentLogLevel = System.getenv("LOG_LEVEL");

        BootstrapLogging.bootstrap(environmentLogLevel != null ? Level.toLevel(environmentLogLevel) : Level.ERROR);

        LogMapping.register();

        String[] disableLogging = new String[]{ "uk.co.jemos.podam",
                                                "com.datastax",
                                                "org.cassandraunit",
                                                "io.netty",
                                                "com.netflix.governator",
                                                "com.hazelcast.nio",
                                                "org.glassfish",
                                                "org.apache"
        };

        Arrays.stream(disableLogging).forEach(i -> {
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(i)).setLevel(Level.OFF);
        });
    }

    @Getter(AccessLevel.PROTECTED)
    private final TestClock testClock = new TestClock();

    public TestBase() {

    }

    protected TestQueueContext createTestQueueContext(QueueName queueName) {
        final TestQueueContext testQueueContext = new TestQueueContext(testAccountName, queueName, getDefaultInjector());

        return testQueueContext;
    }

    @Before
    public void beforeTest() {
        hazelCastModule = new HazelcastTestModule("test_" + UUID.randomUUID());
    }

    @After
    public void afterTest() {
        hazelCastModule.close();
    }

    protected TestQueueContext setupTestContext(QueueDefinition queueDefinition) {
        return new TestQueueContext(createQueue(queueDefinition), getDefaultInjector());
    }

    protected TestQueueContext setupTestContext(String queueName) {
        return setupTestContext(queueName, 20);
    }

    protected TestQueueContext setupTestContext(String queueName, int bucketSize) {
        final QueueName queue = QueueName.valueOf(queueName);
        final QueueDefinition queueDefinition = QueueDefinition.builder()
                                                               .accountName(testAccountName)
                                                               .queueName(queue)
                                                               .bucketSize(BucketSize.valueOf(bucketSize))
                                                               .build();
        return setupTestContext(queueDefinition);
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
                                                                   hazelCastModule,
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
                                                               .accountName(testAccountName)
                                                               .bucketSize(BucketSize.valueOf(bucketSize))
                                                               .repairWorkerPollFrequencySeconds(1)
                                                               .repairWorkerTombstonedBucketTimeoutSeconds(3)
                                                               .build();

        return createQueue(queueDefinition, injector);
    }

    private QueueDefinition createQueue(final QueueDefinition queueDefinition, final Injector injector) {
        final DataContextFactory dataContextFactory = injector.getInstance(DataContextFactory.class);

        final QueueRepository queueRepository = dataContextFactory.forAccount(testAccountName);

        final QueueDefinition createdDefinition = queueRepository.createQueue(queueDefinition).get();

        assertThat(queueRepository.getQueueUnsafe(createdDefinition.getQueueName()).isPresent()).isTrue();

        return createdDefinition;
    }

    protected QueueDefinition createQueue(final QueueDefinition queueDefinition) {
        return createQueue(queueDefinition, getDefaultInjector());
    }
}
