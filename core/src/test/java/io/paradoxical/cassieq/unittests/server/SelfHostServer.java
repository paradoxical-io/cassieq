package io.paradoxical.cassieq.unittests.server;

import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.api.client.CassandraQueueApi;
import io.paradoxical.common.test.guice.OverridableModule;
import io.paradoxical.common.test.web.runner.ServiceTestRunner;
import com.godaddy.logging.Logger;
import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class SelfHostServer {
    private static final Logger logger = getLogger(SelfHostServer.class);

    @Getter
    private final List<OverridableModule> overridableModules;

    private static Random random = new Random();

    private ServiceTestRunner<ServiceConfiguration, TestService> serviceConfigurationTestServiceServiceTestRunner;

    public SelfHostServer(List<OverridableModule> overridableModules) {
        this.overridableModules = overridableModules;
    }


    public SelfHostServer(OverridableModule... overridableModules) {
        this(Arrays.asList(overridableModules));
    }


    public void start(ServiceConfiguration configuration) {


        serviceConfigurationTestServiceServiceTestRunner =
                new ServiceTestRunner<>(TestService::new,
                                        configuration,
                                        getNextPort());

        serviceConfigurationTestServiceServiceTestRunner.run(ImmutableList.copyOf(overridableModules));
    }

    public void start() {
        start(getDefaultConfig());
    }

    public void stop() throws Exception {
        serviceConfigurationTestServiceServiceTestRunner.close();
    }

    protected static long getNextPort() {
        return random.nextInt(35000) + 15000;
    }

    public CassandraQueueApi getClient(String path) {
        return CassandraQueueApi.createClient(getBaseUri().toString());
    }

    public URI getBaseUri() {

        final String uri = String.format("http://localhost:%s/", serviceConfigurationTestServiceServiceTestRunner.getLocalPort());

        return URI.create(uri);
    }

    private ServiceConfiguration getDefaultConfig() {
        return new ServiceConfiguration();
    }
}