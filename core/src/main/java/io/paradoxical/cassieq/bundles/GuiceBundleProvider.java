package io.paradoxical.cassieq.bundles;

import com.google.inject.Module;
import com.hubspot.dropwizard.guice.GuiceBundle;
import io.paradoxical.cassieq.ServiceApplication;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.modules.DefaultApplicationModules;

import java.util.List;

public class GuiceBundleProvider {

    private GuiceBundle<ServiceConfiguration> bundle;

    public GuiceBundleProvider() {
    }

    public synchronized GuiceBundle<ServiceConfiguration> getBundle() {
        if (bundle == null) {
            bundle = buildGuiceBundle();
        }

        return bundle;
    }

    protected List<Module> getModules() {
        return DefaultApplicationModules.getModules();
    }

    private GuiceBundle<ServiceConfiguration> buildGuiceBundle() {
        final GuiceBundle.Builder<ServiceConfiguration> builder = GuiceBundle.<ServiceConfiguration>newBuilder();

        builder.enableAutoConfig(ServiceApplication.class.getPackage().getName())
               .setConfigClass(ServiceConfiguration.class);

        getModules().stream().forEach(builder::addModule);

        final GuiceBundle<ServiceConfiguration> guiceBundle = builder.build();

        return guiceBundle;
    }
}
