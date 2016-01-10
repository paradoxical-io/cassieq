package io.paradoxical.cassieq.election;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import io.paradoxical.cassieq.configurations.ClusteringConfig;
import io.paradoxical.cassieq.model.LeadershipRole;

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
        return tryAquireLock(key, keySetter -> {
            final String currentOwner = keySetter.value();

            // its already us
            if (currentOwner != null && currentOwner.equals(keySetter.localId())) {
                return true;
            }

            // stale or unclaimed
            if (currentOwner == null || !notInCluster(currentOwner)) {
                keySetter.claim();

                return true;
            }


            // its someone else
            return false;
        });
    }

    @Override
    public boolean tryRelinquishLeadership(final LeadershipRole key) {
        return tryAquireLock(key, keySetter -> {
            if (keySetter.amLeader()) {
                keySetter.release();

                return true;
            }

            return false;
        });
    }

    private boolean notInCluster(final String clusterId) {
        return !hazelcastInstance.getCluster()
                                 .getMembers()
                                 .stream()
                                 .map(Member::getUuid)
                                 .collect(toSet())
                                 .contains(clusterId);
    }

    private boolean tryAquireLock(final LeadershipRole key, final Function<KeySetter, Boolean> action) {
        final KeySetter keySetter = new KeySetter(key);

        if (keySetter.tryLock()) {
            try {
                return action.apply(keySetter);
            }
            finally {
                keySetter.unlock();
            }
        }

        return false;
    }

    class KeySetter {
        private final IMap<LeadershipRole, String> backingMap;
        private final LeadershipRole key;

        public KeySetter(LeadershipRole key) {
            this.backingMap = hazelcastInstance.getMap("leaders");

            this.key = key;
        }

        private String localId() {
            return hazelcastInstance.getCluster().getLocalMember().getUuid();
        }

        public boolean amLeader() {
            return value().equals(localId());
        }

        public String value() {
            return backingMap.get(key);
        }

        public void claim() {
            backingMap.put(key, localId());
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
