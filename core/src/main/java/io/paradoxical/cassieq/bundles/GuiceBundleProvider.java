package io.paradoxical.cassieq.bundles;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.hubspot.dropwizard.guice.GuiceBundle;
import com.hubspot.dropwizard.guice.InjectorFactory;
import com.netflix.governator.Governator;
import io.paradoxical.cassieq.ServiceApplication;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.discoverable.GuiceDiscoverablePackageMarker;
import io.paradoxical.cassieq.modules.DefaultApplicationModules;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.List;

public class GuiceBundleProvider {

    public static final InjectorFactory DefaultInjectorFactory = (stage, modules) -> Governator.createInjector(modules);
    private final InjectorFactory injectorFactory;

    private GuiceBundle<ServiceConfiguration> bundle;

    public Injector getInjector() {
        return getBundle().getInjector();
    }

    public GuiceBundleProvider() {
        this(DefaultInjectorFactory);
    }

    public GuiceBundleProvider(InjectorFactory injectorFactory) {
        this.injectorFactory = injectorFactory;
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

        builder.enableAutoConfig(GuiceDiscoverablePackageMarker.class.getPackage().getName())
               .setConfigClass(ServiceConfiguration.class)
               .setInjectorFactory(injectorFactory);

        getModules().stream().forEach(builder::addModule);

        return builder.build();
    }
}
