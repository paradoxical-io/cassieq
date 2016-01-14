package io.paradoxical.cassieq.unittests.server;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.hubspot.dropwizard.guice.InjectorFactory;
import io.dropwizard.setup.Environment;
import io.paradoxical.cassieq.ServiceApplication;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.bundles.GuiceBundleProvider;
import io.paradoxical.common.test.guice.ModuleOverrider;
import io.paradoxical.common.test.guice.ModuleUtils;
import io.paradoxical.common.test.guice.OverridableModule;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestService extends ServiceApplication implements ModuleOverrider {

    private final CountDownLatch runLatch = new CountDownLatch(1);

    public void waitForRun() throws InterruptedException {
        runLatch.await();
    }

    @Override
    public void run(final ServiceConfiguration config, final Environment env) throws Exception {
        super.run(config, env);
        runLatch.countDown();
    }

    public TestService(final List<OverridableModule> modules) {
        super(new TestGuiceBundleProvier(modules));
    }

    @Override
    public void overrideModulesWith(final List<OverridableModule> modules) {

    }

    @Override
    public List<OverridableModule> getOverrideModules() {
        return ((TestGuiceBundleProvier) getGuiceBundleProvider()).getOverridableModules();
    }

    public static class TestGuiceBundleProvier extends GuiceBundleProvider {
        @Getter
        private List<OverridableModule> overridableModules;

        public TestGuiceBundleProvier(final List<OverridableModule> overridableModules) {
            this.overridableModules = overridableModules;
        }

        @Override
        protected List<Module> getModules() {
            return ModuleUtils.mergeModules(super.getModules(), overridableModules);
        }
    }
}
