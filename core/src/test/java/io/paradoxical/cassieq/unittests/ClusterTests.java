package io.paradoxical.cassieq.unittests;

import com.hazelcast.core.HazelcastInstance;
import io.paradoxical.cassieq.unittests.modules.HazelcastTestModule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterTests {
    @Test
    public void test_hazelcasts(){
        final HazelcastInstance instance1 = new HazelcastTestModule().getInstance();
        final HazelcastInstance instance2 = new HazelcastTestModule().getInstance();

        assertThat(instance2.getCluster().getMembers().size()).isEqualTo(2);

        instance1.shutdown();

        assertThat(instance2.getCluster().getMembers().size()).isEqualTo(1);
    }
}
