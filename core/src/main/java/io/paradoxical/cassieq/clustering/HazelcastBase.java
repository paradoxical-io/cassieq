package io.paradoxical.cassieq.clustering;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import io.paradoxical.cassieq.model.ClusterMember;

import static java.util.stream.Collectors.toSet;

public abstract class HazelcastBase {

    private final HazelcastInstance hazelcastInstance;

    public HazelcastBase(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    protected boolean inCluster(final ClusterMember clusterId) {
        return hazelcastInstance.getCluster()
                                .getMembers()
                                .stream()
                                .map(Member::getUuid)
                                .map(ClusterMember::valueOf)
                                .collect(toSet())
                                .contains(clusterId);
    }


    protected ClusterMember thisClusterMember() {
        return ClusterMember.valueOf(hazelcastInstance.getCluster().getLocalMember().getUuid());
    }
}
