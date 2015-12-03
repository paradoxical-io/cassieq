package io.paradoxical.cassieq.unittests.server;

import com.google.inject.Module;
import io.paradoxical.cassieq.ServiceApplication;
import io.paradoxical.cassieq.bundles.GuiceBundleProvider;
import io.paradoxical.common.test.guice.ModuleUtils;
import io.paradoxical.common.test.guice.OverridableModule;

import java.util.List;

public class TestService extends ServiceApplication {
    public TestService(final List<OverridableModule> modules) {
        super(new TestGuiceBundleProvier(modules));
    }

    public static class TestGuiceBundleProvier extends GuiceBundleProvider {
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
