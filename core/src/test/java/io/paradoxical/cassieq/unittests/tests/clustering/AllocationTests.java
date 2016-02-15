package io.paradoxical.cassieq.unittests.tests.clustering;

import com.google.inject.Injector;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.clustering.allocation.ResourceAllocator;
import io.paradoxical.cassieq.clustering.allocation.ResourceConfig;
import io.paradoxical.cassieq.clustering.allocation.ResourceIdentity;
import io.paradoxical.cassieq.configurations.AllocationConfig;
import io.paradoxical.cassieq.configurations.AllocationStrategy;
import io.paradoxical.cassieq.unittests.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class AllocationTests extends TestBase {

    private final int instanceNumber;
    private final int clusterSize;
    private final int totalAllocatable;
    private final Integer[] expectedResult;

    public AllocationTests(int instanceNumber, int clusterSize, int totalAllocatable, Integer[] expectedResult){
        this.instanceNumber = instanceNumber;
        this.clusterSize = clusterSize;
        this.totalAllocatable = totalAllocatable;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters(name = "Instance {0}, ClusterSize {1}, TotalToDistribute {2}")
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { 0, 3, 10, new Integer[]{ 0, 1, 2, }},
                { 1, 3, 10, new Integer[]{ 3, 4, 5,  }},
                { 2, 3, 10, new Integer[]{ 6, 7, 8, 9 }}
        });
    }

    @Test
    public void test_passthrough_allocation() {
        final ServiceConfiguration serviceConfiguration = new ServiceConfiguration();

        final AllocationConfig allocationConfig = new AllocationConfig();

        allocationConfig.setStrategy(AllocationStrategy.MANUAL);

        allocationConfig.setManualAllocatorsCount(clusterSize);

        allocationConfig.setManualAllocatorInstanceNumber(instanceNumber);

        serviceConfiguration.setAllocationConfig(allocationConfig);

        Set<Integer> allocated = allocate(totalAllocatable, serviceConfiguration);

        assertThat(allocated.toArray()).isEqualTo(expectedResult);
    }

    private Set<Integer> allocate(final int totalResources, final ServiceConfiguration serviceConfiguration) {
        final Injector injector = getDefaultInjector(serviceConfiguration);

        final ResourceAllocator.Factory instance = injector.getInstance(ResourceAllocator.Factory.class);

        final Set<ResourceIdentity> inputData = IntStream.range(0, totalResources).mapToObj(i -> new ResourceIdentity(String.valueOf(i))).collect(toSet());

        final Set<ResourceIdentity> allocatedData = new HashSet<>();

        final ResourceAllocator allocator = instance.getAllocator(fixture.manufacturePojo(ResourceConfig.class),
                                                                  () -> inputData,
                                                                  allocatedData::addAll);


        allocator.claim();

        return allocatedData.stream().map(i -> Integer.valueOf(i.get())).collect(Collectors.toSet());
    }
}
