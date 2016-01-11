package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import io.paradoxical.cassieq.configurations.ClusteringConfig;
import io.paradoxical.cassieq.clustering.election.HazelcastLeadershipProvider;
import io.paradoxical.cassieq.clustering.election.LeadershipProvider;
import io.paradoxical.cassieq.model.LeadershipRole;

public class LeadershipModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    public LeadershipProvider leadershipProvider(ClusteringConfig config, Injector injector) {
        if (config.isEnabled()) {
            return injector.getInstance(HazelcastLeadershipProvider.class);
        }

        return getBypassedLeadershipProvider();
    }

    private LeadershipProvider getBypassedLeadershipProvider() {
        return new LeadershipProvider() {
            @Override
            public boolean tryAcquireLeader(final LeadershipRole key) {
                return true;
            }

            @Override
            public boolean tryRelinquishLeadership(final LeadershipRole key) {
                return true;
            }
        };
    }
}
