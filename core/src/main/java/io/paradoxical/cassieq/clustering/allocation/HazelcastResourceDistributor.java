package io.paradoxical.cassieq.clustering.allocation;

import com.godaddy.logging.Logger;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import io.paradoxical.cassieq.clustering.HazelcastBase;
import io.paradoxical.cassieq.configurations.ClusteringConfig;
import io.paradoxical.cassieq.model.ClusterMember;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toSet;

public class HazelcastResourceDistributor extends HazelcastBase implements ResourceAllocator {
    private final HazelcastInstance hazelcastInstance;
    private final ClusteringConfig clusteringConfig;
    private final ResourceConfig config;
    private final Supplier<Set<ResourceIdentity>> inputSupplier;
    private final Consumer<Set<ResourceIdentity>> allocationEvent;

    private static final Logger logger = getLogger(HazelcastResourceDistributor.class);

    @Inject
    public HazelcastResourceDistributor(
            HazelcastInstance hazelcastInstance,
            ClusteringConfig clusteringConfig,
            @Assisted ResourceConfig config,
            @Assisted Supplier<Set<ResourceIdentity>> inputSupplier,
            @Assisted Consumer<Set<ResourceIdentity>> onDistributed) {
        super(hazelcastInstance);
        this.hazelcastInstance = hazelcastInstance;
        this.clusteringConfig = clusteringConfig;
        this.config = config;
        this.inputSupplier = inputSupplier;
        this.allocationEvent = onDistributed;

        hazelcastInstance.getCluster().addMembershipListener(getMembershipListener());
    }

    private MembershipListener getMembershipListener() {
        return new MembershipListener() {
            @Override
            public void memberAdded(final MembershipEvent membershipEvent) {
                claim();
            }

            @Override
            public void memberRemoved(final MembershipEvent membershipEvent) {
                removeClaimedResources(ClusterMember.valueOf(membershipEvent.getMember().getUuid()));

                claim();
            }

            @Override
            public void memberAttributeChanged(final MemberAttributeEvent memberAttributeEvent) {

            }
        };
    }

    private void removeClaimedResources(final ClusterMember clusterMember) {
        final IMap<ResourceGroup, HashMap<ClusterMember, Set<ResourceIdentity>>> groupAllocationMap = getMap();

        try {
            if (lockOnMap(groupAllocationMap)) {
                try {
                    final HashMap<ClusterMember, Set<ResourceIdentity>> allocatedResources = groupAllocationMap.get(config.getGroup());

                    allocatedResources.remove(clusterMember);

                    // persist back to shared state
                    groupAllocationMap.put(config.getGroup(), allocatedResources);
                }
                finally {
                    groupAllocationMap.unlock(config.getGroup());
                }
            }
        }
        catch (InterruptedException e) {
            logger.error(e, "Error synchronizing cluster");
        }
    }

    @Override
    public void claim() {

        final IMap<ResourceGroup, HashMap<ClusterMember, Set<ResourceIdentity>>> groupAllocationMap = getMap();

        try {
            if (lockOnMap(groupAllocationMap)) {
                try {
                    final Set<ResourceIdentity> total = inputSupplier.get();

                    final HashMap<ClusterMember, Set<ResourceIdentity>> allocated = groupAllocationMap.get(config.getGroup());

                    final Set<ResourceIdentity> claimed = claimResources(total, allocated);

                    allocated.put(thisClusterMember(), claimed);

                    groupAllocationMap.set(config.getGroup(), allocated);

                    allocationEvent.accept(claimed);
                }
                finally {
                    groupAllocationMap.unlock(config.getGroup());
                }
            }
        }
        catch (InterruptedException e) {
            logger.error(e, "Error synchronizing cluster");
        }
    }

    private boolean lockOnMap(final IMap<ResourceGroup, HashMap<ClusterMember, Set<ResourceIdentity>>> groupAllocationMap) throws InterruptedException {
        return groupAllocationMap.tryLock(config.getGroup(), clusteringConfig.getLockWaitSeconds(), TimeUnit.SECONDS);
    }

    private IMap<ResourceGroup, HashMap<ClusterMember, Set<ResourceIdentity>>> getMap() {
        return  hazelcastInstance.getMap(config.getGroup().get());
    }

    private Set<ResourceIdentity> claimResources(final Set<ResourceIdentity> total, final HashMap<ClusterMember, Set<ResourceIdentity>> allocated) {
        final Set<ResourceIdentity> availablePool = totalAvailableForClaiming(allocated, total);

        final int clusterSize = hazelcastInstance.getCluster().getMembers().size();

        final int perClusterMemberMax = Double.valueOf(Math.ceil(availablePool.size() / (double) clusterSize)).intValue();

        final Set<ResourceIdentity> currentlyAllocatedToMe = allocated.get(thisClusterMember());

        // gotta fill resources
        if(currentlyAllocatedToMe.size() <= perClusterMemberMax){
            final Set<ResourceIdentity> differenceToFill = availablePool.stream()
                                                                        .limit(perClusterMemberMax - currentlyAllocatedToMe.size())
                                                                        .collect(toSet());

            return Sets.union(currentlyAllocatedToMe, differenceToFill);
        }
        // gotta give up resources
        else {
            final int amountToGiveUp = currentlyAllocatedToMe.size() - perClusterMemberMax;

            return currentlyAllocatedToMe.stream().skip(amountToGiveUp).collect(Collectors.toSet());
        }
    }

    private Set<ResourceIdentity> totalAvailableForClaiming(final HashMap<ClusterMember, Set<ResourceIdentity>> allocatedResources, final Set<ResourceIdentity> total) {
        final Set<ResourceIdentity> takenResources = allocatedResources.values().stream()
                                                                       .flatMap(Collection::stream)
                                                                       .collect(toSet());

        return Sets.difference(takenResources, total).immutableCopy();
    }

    class EntityListener implements EntryAddedListener<ClusterMember, Set<ResourceIdentity>>, EntryRemovedListener<ClusterMember, Set<ResourceIdentity>> {

        private final Consumer<Set<ResourceIdentity>> onDistributed;

        public EntityListener(Consumer<Set<ResourceIdentity>> onDistributed) {

            this.onDistributed = onDistributed;
        }

        /**
         * Invoked upon addition of an entry.
         *
         * @param event the event invoked when an entry is added
         */
        @Override
        public void entryAdded(final EntryEvent<ClusterMember, Set<ResourceIdentity>> event) {
            onDistributed.accept(event.getValue());
        }

        /**
         * Invoked upon removal of an entry.
         *
         * @param event the event invoked when an entry is removed
         */
        @Override
        public void entryRemoved(final EntryEvent<ClusterMember, Set<ResourceIdentity>> event) {
            onDistributed.accept(event.getValue());
        }
    }
}
