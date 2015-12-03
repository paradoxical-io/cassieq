package io.paradoxical.cassieq.unittests;

import io.paradoxical.cassieq.workers.LeaderBasedRepairWorker;
import com.google.inject.Injector;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepairWorkerManagerTester extends TestBase {

    @Test
    @Ignore
    public void test_leader() throws Exception {
        final Injector defaultInjector = getDefaultInjector();

        final LeaderBasedRepairWorker worker1 = defaultInjector.getInstance(LeaderBasedRepairWorker.class);
        final LeaderBasedRepairWorker worker2 = defaultInjector.getInstance(LeaderBasedRepairWorker.class);
        final LeaderBasedRepairWorker worker3 = defaultInjector.getInstance(LeaderBasedRepairWorker.class);

        new Thread(new Runnable() {
            @Override
            public void run() {
                worker1.start();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                worker2.start();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                worker3.start();
            }
        }).start();

        int iteration = 0;
        while (!worker1.isLeader() && !worker2.isLeader() && !worker3.isLeader() && iteration < 10) {
            Thread.sleep(100);

            iteration++;
        }

        assertThat(worker1.isLeader() || worker2.isLeader() || worker3.isLeader()).isTrue();
    }

}
