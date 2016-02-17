package io.paradoxical.cassieq.bundles;

import com.datastax.driver.core.Session;
import com.google.inject.Module;
import io.paradoxical.cassandra.loader.db.CqlUnitDb;
import io.paradoxical.cassieq.environment.SystemProps;
import io.paradoxical.cassieq.modules.SessionProviderModule;
import io.paradoxical.common.test.guice.ModuleUtils;
import io.paradoxical.common.test.guice.OverridableModule;

import java.util.Arrays;
import java.util.List;

public class DevGuiceBundleProvider extends GuiceBundleProvider {
    @Override
    protected List<Module> getModules() {
        return ModuleUtils.mergeModules(super.getModules(), Arrays.asList(new OverridableModule() {
            @Override
            public Class<? extends Module> getOverridesModule() {
                return SessionProviderModule.class;
            }

            @Override
            protected void configure() {
                try {
                    final String db_path = SystemProps.instance().DB_SCRIPTS_PATH();

                    bind(Session.class).toInstance(CqlUnitDb.create(db_path == null ? "/data/db" : db_path));
                }
                catch (Exception e) {
                    throw new RuntimeException("Cannot create session!", e);
                }
            }
        }));
    }
}
