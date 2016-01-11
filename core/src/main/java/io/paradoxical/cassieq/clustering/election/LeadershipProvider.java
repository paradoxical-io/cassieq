package io.paradoxical.cassieq.clustering.election;

import io.paradoxical.cassieq.model.LeadershipRole;

public interface LeadershipProvider {
    boolean tryAcquireLeader(LeadershipRole key);

    boolean tryRelinquishLeadership(LeadershipRole key);
}
