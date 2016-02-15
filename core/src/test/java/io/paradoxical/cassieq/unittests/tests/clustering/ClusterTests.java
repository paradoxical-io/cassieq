package io.paradoxical.cassieq.unittests.tests.clustering;

import com.hazelcast.core.HazelcastInstance;
import io.paradoxical.cassieq.unittests.modules.HazelcastTestModule;
import lombok.Cleanup;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterTests {
    @Test
    public void test_hazelcasts(){
        @Cleanup("shutdown") final HazelcastInstance instance1 = new HazelcastTestModule("test_hazelcasts").getInstance();
        @Cleanup("shutdown") final HazelcastInstance instance2 = new HazelcastTestModule("test_hazelcasts").getInstance();

        assertThat(instance2.getCluster().getMembers().size()).isEqualTo(2);

        instance1.shutdown();

        assertThat(instance2.getCluster().getMembers().size()).isEqualTo(1);
    }
}
