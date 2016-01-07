package io.paradoxical.cassieq.election;

import io.paradoxical.cassieq.model.LeadershipRole;

public interface LeadershipProvider {
    boolean tryAcquireLeader(LeadershipRole key);
}
