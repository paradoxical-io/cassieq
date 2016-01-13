package io.paradoxical.cassieq.configurations;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AllocationConfig {
    @NotNull
    private AllocationStrategy strategy = AllocationStrategy.CLUSTER;

    /**
     * If manual strategy is chosen, we can determine how many total allocators there SHOULD be
     *
     * i.e. you expect 10 instances to be running, so set this to be 10.
     */
    private Integer manualAllocatorsCount;

    /**
     * if manual allocation is selected, this will tell the allocator which instance it is out of the group.
     *
     * If 10 are selected, this instance may be pinned at instance 2 for example.
     */
    private Integer manualAllocatorInstanceNumber;
}
