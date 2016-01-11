package io.paradoxical.cassieq.unittests.modules;

import com.google.inject.Module;
import io.paradoxical.cassieq.clustering.election.LeadershipProvider;
import io.paradoxical.cassieq.model.LeadershipRole;
import io.paradoxical.cassieq.modules.LeadershipModule;
import io.paradoxical.common.test.guice.OverridableModule;

public class MockLeadershipProviderModule extends OverridableModule {
    @Override
    public Class<? extends Module> getOverridesModule() {
        return LeadershipModule.class;
    }

    /**
     * Configures a {@link Binder} via the exposed methods.
     */
    @Override
    protected void configure() {
        bind(LeadershipProvider.class).toInstance(new LeadershipProvider() {
            @Override
            public boolean tryAcquireLeader(final LeadershipRole key) {
                return true;
            }

            @Override
            public boolean tryRelinquishLeadership(final LeadershipRole key) {
                return true;
            }
        });
    }
}
