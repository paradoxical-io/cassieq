package io.paradoxical.cassieq.clustering.allocation;

import com.godaddy.logging.Logger;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import io.paradoxical.cassieq.clustering.HazelcastBase;
import io.paradoxical.cassieq.configurations.ClusteringConfig;
import io.paradoxical.cassieq.model.ClusterMember;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toSet;

public class HazelcastResourceAllocater extends HazelcastBase implements ResourceAllocator {
    private final HazelcastInstance hazelcastInstance;

    private final ClusteringConfig clusteringConfig;

    private final ResourceConfig config;

    private final Supplier<Set<ResourceIdentity>> inputSupplier;

    private final Consumer<Set<ResourceIdentity>> allocationEvent;

    private final String hazelcastMembershipListenerId;

    private static final Logger logger = getLogger(HazelcastResourceAllocater.class);

    @Inject
    public HazelcastResourceAllocater(
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

        hazelcastMembershipListenerId = hazelcastInstance.getCluster().addMembershipListener(getMembershipListener());
    }

    @Override
    public void close() throws Exception {
        hazelcastInstance.getCluster().removeMembershipListener(hazelcastMembershipListenerId);
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
        final IMap<ResourceGroup, Map<ClusterMember, Set<ResourceIdentity>>> groupAllocationMap = getMap();

        try {
            if (lockOnMap(groupAllocationMap)) {
                try {
                    final Map<ClusterMember, Set<ResourceIdentity>> allocatedResources = groupAllocationMap.get(config.getGroup());

                    if (!allocatedResources.containsKey(clusterMember)) {
                        return;
                    }

                    allocatedResources.remove(clusterMember);

                    logger.with("member-id", clusterMember)
                          .info("Removing claimed resources since they have left");

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

        Set<ResourceIdentity> claimed = Sets.newHashSet();

        // because we need to go through everyone in the cluster
        // put the ownership on one node for this group
        // otherwise calling the entry set across a large clutster would be huge
        // we may in the future want to change this depending on the experimeintal cluster size
        final IMap<ResourceGroup, Map<ClusterMember, Set<ResourceIdentity>>> groupAllocationMap = getMap();

        try {
            final Logger groupLogger = logger.with("group", config.getGroup());

            groupLogger.info("Trying to acquire lock");

            if (lockOnMap(groupAllocationMap)) {
                try {
                    groupLogger.success("Acquired lock");

                    final Set<ResourceIdentity> total = inputSupplier.get();

                    Map<ClusterMember, Set<ResourceIdentity>> allocated = groupAllocationMap.get(config.getGroup());

                    if (allocated == null) {
                        allocated = new HashMap<>();
                    }

                    logger.info("Allocated existing = " + allocated);

                    claimed = claimResources(total, allocated);

                    allocated.put(thisClusterMember(), claimed);

                    groupAllocationMap.set(config.getGroup(), allocated);

                    logger.with("member-id", thisClusterMember())
                          .with("num-allocated", claimed.size())
                          .success("Claimed");
                }
                finally {
                    groupAllocationMap.unlock(config.getGroup());

                    groupLogger.success("Released lock");

                    allocationEvent.accept(claimed);
                }
            }
            else {
                groupLogger.warn("Unable to claim group lock!");
            }
        }
        catch (InterruptedException e) {
            logger.error(e, "Error synchronizing cluster");
        }
    }

    private boolean lockOnMap(final IMap<ResourceGroup, Map<ClusterMember, Set<ResourceIdentity>>> groupAllocationMap) throws InterruptedException {
        return groupAllocationMap.tryLock(config.getGroup(), clusteringConfig.getLockWaitSeconds(), TimeUnit.SECONDS);
    }

    private IMap<ResourceGroup, Map<ClusterMember, Set<ResourceIdentity>>> getMap() {
        return hazelcastInstance.getMap(config.getGroup().get());
    }

    private Set<ResourceIdentity> claimResources(final Set<ResourceIdentity> total, final Map<ClusterMember, Set<ResourceIdentity>> allocated) {
        final Set<ResourceIdentity> availablePool = totalAvailableForClaiming(allocated, total);

        final int clusterSize = hazelcastInstance.getCluster().getMembers().size();

        final int perClusterMemberClaimMax = Double.valueOf(Math.ceil(total.size() / (double) clusterSize)).intValue();

        final Set<ResourceIdentity> currentlyAllocatedToMe = getCurrentAllocations(allocated, thisClusterMember());

        final Set<ResourceIdentity> resourcesNoLongerActiveButClaimedByMe = Sets.difference(currentlyAllocatedToMe, total).immutableCopy();

        if (resourcesNoLongerActiveButClaimedByMe.size() > 0) {
            logger.with("member-id", thisClusterMember())
                  .with("inactive-claim-number", resourcesNoLongerActiveButClaimedByMe.size())
                  .warn("Has claimed resources that are no longer active. Relinquishing");

            currentlyAllocatedToMe.removeAll(resourcesNoLongerActiveButClaimedByMe);
        }

        Logger resourceLogger = logger.with("member-max-claim-num", perClusterMemberClaimMax)
                                      .with("cluster-size", clusterSize)
                                      .with("current-allocations-to-this", currentlyAllocatedToMe.size());

        // can't take any more, and we're as good as its gonna get
        if (currentlyAllocatedToMe.size() == perClusterMemberClaimMax) {
            return currentlyAllocatedToMe;
        }

        // gotta fill resources
        if (currentlyAllocatedToMe.size() < perClusterMemberClaimMax) {
            final Set<ResourceIdentity> differenceToFill = availablePool.stream()
                                                                        .limit(perClusterMemberClaimMax - currentlyAllocatedToMe.size())
                                                                        .collect(toSet());

            resourceLogger.with("claiming-count", differenceToFill.size()).info("Claiming");

            return Sets.union(currentlyAllocatedToMe, differenceToFill).immutableCopy();
        }
        // gotta give up resources
        else {
            final int amountToGiveUp = currentlyAllocatedToMe.size() - perClusterMemberClaimMax;

            resourceLogger.with("releasing-resource-count", amountToGiveUp).info("Releasing");

            return currentlyAllocatedToMe.stream().skip(amountToGiveUp).collect(Collectors.toSet());
        }
    }

    private Set<ResourceIdentity> getCurrentAllocations(final Map<ClusterMember, Set<ResourceIdentity>> allocated, final ClusterMember clusterMember) {
        if (MapUtils.isEmpty(allocated)) {
            return Sets.newHashSet();
        }

        final Set<ResourceIdentity> resourceIdentities = allocated.get(clusterMember);

        if (CollectionUtils.isEmpty(resourceIdentities)) {
            return Sets.newHashSet();
        }

        return new HashSet<>(resourceIdentities);
    }

    private Set<ResourceIdentity> totalAvailableForClaiming(final Map<ClusterMember, Set<ResourceIdentity>> allocatedResources, final Set<ResourceIdentity> total) {
        if (MapUtils.isEmpty(allocatedResources)) {
            return total;
        }

        final Set<ResourceIdentity> takenResources = allocatedResources.values().stream()
                                                                       .flatMap(Collection::stream)
                                                                       .collect(toSet());

        return Sets.difference(total, takenResources).immutableCopy();
    }
}
