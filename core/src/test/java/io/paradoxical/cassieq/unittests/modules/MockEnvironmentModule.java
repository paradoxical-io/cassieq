package io.paradoxical.cassieq.unittests.modules;

import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.common.test.guice.OverridableModule;
import com.google.inject.Module;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import org.mockito.Mockito;

/**
 * Can be used to mock out dropwizard environment and auto mock the config class for a dropwizard service
 */
public class MockEnvironmentModule extends OverridableModule {

    @Getter
    private final ServiceConfiguration configInstance;

    @Getter private Environment mockEnvironment = Mockito.mock(Environment.class);


    public MockEnvironmentModule(ServiceConfiguration config) {
        configInstance = config;
    }

    @Override public Class<? extends Module> getOverridesModule() {
        return null;
    }

    @Override protected void configure() {
        bind(Environment.class).toInstance(mockEnvironment);

        bind(ServiceConfiguration.class).toInstance(configInstance);
    }
}

