package io.paradoxical.cassieq.unittests;

import io.paradoxical.cassieq.api.client.CassieqApi;
import io.paradoxical.cassieq.unittests.modules.HazelcastTestModule;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.server.SelfHostServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public abstract class ApiTestsBase extends DbTestBase {

    protected static SelfHostServer server;

    protected static CassieqApi client;

    @BeforeClass
    public static void setup() throws InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        server = new SelfHostServer(new InMemorySessionProvider(session), new HazelcastTestModule("api-tester"));

        server.start();

        server.getService().waitForRun();

        client = CassieqApi.createClient(server.getBaseUri().toString(),
                                         getTestAccountCredentials(server.getService().getGuiceBundleProvider().getInjector()));
    }

    @AfterClass
    public static void cleanup() {
        server.stop();
    }
}
