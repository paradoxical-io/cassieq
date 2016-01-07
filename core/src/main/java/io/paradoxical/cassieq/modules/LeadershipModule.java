package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import io.paradoxical.cassandra.leadership.LeadershipStatusImpl;
import io.paradoxical.cassandra.leadership.data.LeadershipSchema;
import io.paradoxical.cassandra.leadership.factories.CassandraLeadershipElectionFactory;
import io.paradoxical.cassandra.leadership.interfaces.LeadershipElectionFactory;
import io.paradoxical.cassandra.leadership.interfaces.LeadershipStatus;

public class LeadershipModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LeadershipSchema.class).toInstance(LeadershipSchema.Default);

        bind(LeadershipStatus.class).to(LeadershipStatusImpl.class);

        bind(LeadershipElectionFactory.class).to(CassandraLeadershipElectionFactory.class);
    }
}
