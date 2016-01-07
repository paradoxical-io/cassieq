package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import io.paradoxical.cassieq.election.HazelcastLeadershipProvider;
import io.paradoxical.cassieq.election.LeadershipProvider;

public class LeadershipModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LeadershipProvider.class).to(HazelcastLeadershipProvider.class);
    }
}
