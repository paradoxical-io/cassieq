package io.paradoxical.cassieq.unittests.tests.api;

import com.google.inject.Injector;
import io.paradoxical.cassieq.api.client.CassieqApi;
import io.paradoxical.cassieq.model.time.Clock;
import io.paradoxical.cassieq.unittests.DbTestBase;
import io.paradoxical.cassieq.unittests.modules.HazelcastTestModule;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.modules.TestClockModule;
import io.paradoxical.cassieq.unittests.server.SelfHostServer;
import io.paradoxical.cassieq.unittests.time.TestClock;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public abstract class ApiTestsBase extends DbTestBase {

    protected static SelfHostServer server;

    private final Injector injector;

    public ApiTestsBase() {
        injector = server.getService().getGuiceBundleProvider().getInjector();

        this.apiClient =
                CassieqApi.createClient(
                        server.getBaseUri().toString(),
                        getTestAccountCredentials(injector));
    }

    @Getter(AccessLevel.PROTECTED)
    @Accessors(fluent = true)

    private final CassieqApi apiClient;

    @BeforeClass
    public static void setup() throws InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        server = new SelfHostServer(new InMemorySessionProvider(session),
                                    new HazelcastTestModule("api-tester"),
                                    new TestClockModule(new TestClock()));

        server.start();

        server.getService().waitForRun();
    }

    @Override
    protected TestClock getTestClock() {
        return ((TestClock) injector.getInstance(Clock.class));
    }

    @AfterClass
    public static void cleanup() {
        server.stop();
    }
}
