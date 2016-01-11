package io.paradoxical.cassieq.clustering.election;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import io.paradoxical.cassieq.configurations.ClusteringConfig;
import io.paradoxical.cassieq.model.ClusterMember;
import io.paradoxical.cassieq.model.LeadershipRole;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toSet;

public class HazelcastLeadershipProvider implements LeadershipProvider {
    private final HazelcastInstance hazelcastInstance;
    private final ClusteringConfig config;

    private static final Logger logger = getLogger(HazelcastLeadershipProvider.class);


    @Inject
    public HazelcastLeadershipProvider(
            HazelcastInstance hazelcastInstance,
            ClusteringConfig config) {
        this.hazelcastInstance = hazelcastInstance;
        this.config = config;
    }

    @Override
    public boolean tryAcquireLeader(final LeadershipRole key) {
        Optional<Boolean> wasExecuted = clusterSyncedExecute(key, keySetter -> {
            final ClusterMember currentOwner = keySetter.value();

            // its already us
            if (currentOwner != null && currentOwner.equals(thisClusterMember())) {
                return true;
            }

            // stale or unclaimed
            if (currentOwner == null || !inCluster(currentOwner)) {
                keySetter.claim();

                return true;
            }

            // its someone else
            return false;
        });

        if (wasExecuted.isPresent()) {
            return wasExecuted.get();
        }

        return false;
    }

    @Override
    public boolean tryRelinquishLeadership(final LeadershipRole key) {
        Optional<Boolean> wasExecuted = clusterSyncedExecute(key, keySetter -> {
            if (keySetter.amLeader()) {
                keySetter.release();

                return true;
            }

            return false;
        });

        if (wasExecuted.isPresent()) {
            return wasExecuted.get();
        }

        return false;
    }

    private boolean inCluster(final ClusterMember clusterId) {
        return hazelcastInstance.getCluster()
                                .getMembers()
                                .stream()
                                .map(Member::getUuid)
                                .map(ClusterMember::valueOf)
                                .collect(toSet())
                                .contains(clusterId);
    }

    private <T> Optional<T> clusterSyncedExecute(final LeadershipRole key, final Function<KeySetter, T> action) {
        final KeySetter keySetter = new KeySetter(key);

        if (keySetter.tryLock()) {
            try {
                return Optional.of(action.apply(keySetter));
            }
            finally {
                keySetter.unlock();
            }
        }

        return Optional.empty();
    }


    private ClusterMember thisClusterMember() {
        return ClusterMember.valueOf(hazelcastInstance.getCluster().getLocalMember().getUuid());
    }

    class KeySetter {
        private final IMap<LeadershipRole, String> backingMap;
        private final LeadershipRole key;

        public KeySetter(LeadershipRole key) {
            this.backingMap = hazelcastInstance.getMap("leaders");

            this.key = key;
        }

        public boolean amLeader() {
            return value().equals(thisClusterMember());
        }

        public ClusterMember value() {
            final String currentValue = backingMap.get(key);

            if (currentValue == null) {
                return null;
            }

            return ClusterMember.valueOf(currentValue);
        }

        public void claim() {
            backingMap.put(key, thisClusterMember().get());
        }

        public boolean tryLock() {
            try {
                if (!backingMap.tryLock(key, config.getLockWaitSeconds(), TimeUnit.SECONDS)) {
                    logger.with("wait-seconds", config.getLockWaitSeconds())
                          .with("key", key)
                          .warn("Unable to acquire lock in time");

                    return false;
                }
            }
            catch (InterruptedException e) {
                logger.error(e, "Error acquiring lock");

                return false;
            }

            return true;
        }

        public void unlock() {
            backingMap.unlock(key);
        }

        public void release() {
            backingMap.remove(key);
        }
    }
}
