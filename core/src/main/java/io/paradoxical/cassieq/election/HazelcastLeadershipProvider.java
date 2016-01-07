package io.paradoxical.cassieq.election;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.Member;
import io.paradoxical.cassieq.model.LeadershipRole;
import lombok.Cleanup;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toSet;

public class HazelcastLeadershipProvider implements LeadershipProvider {
    private final HazelcastInstance hazelcastInstance;

    private static final Logger logger = getLogger(HazelcastLeadershipProvider.class);


    @Inject
    public HazelcastLeadershipProvider(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public boolean tryAcquireLeader(final LeadershipRole key) {

        @Cleanup("unlock") final ILock lock = hazelcastInstance.getLock(key.get());

        int waitSeconds = 5;

        try {
            if (!lock.tryLock(waitSeconds, TimeUnit.SECONDS)) {
                logger.with("wait-seconds", waitSeconds)
                      .with("key", key)
                      .warn("Unable to acquire lock in time");

                return false;
            }
        }
        catch (InterruptedException e) {
            logger.error(e, "Error acquiring lock");

            return false;
        }

        final String localId = hazelcastInstance.getCluster().getLocalMember().getUuid();

        // a single element list containing the cluster member id who owns this key
        final IList<String> owningIds = hazelcastInstance.getList(key.get());

        if (owningIds.contains(localId)) {
            return true;
        }

        if (CollectionUtils.isEmpty(owningIds)) {
            owningIds.add(localId);

            return true;
        }

        final Set<String> members = hazelcastInstance.getCluster().getMembers().stream().map(Member::getUuid).collect(toSet());

        // whoever owned this before isn't in the cluster anymore
        if (!members.contains(owningIds.get(0))) {
            owningIds.set(0, localId);

            return true;
        }

        return false;
    }
}
