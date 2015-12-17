package io.paradoxical.cassieq.unittests;

import io.paradoxical.cassieq.model.time.QueryableScheduledFuture;
import io.paradoxical.cassieq.unittests.time.TestClock;
import io.paradoxical.cassieq.unittests.time.TestExecutorService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Ignore("Not working")
@RunWith(Parameterized.class)
public class TimeTests {
    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[100][0]);
    }

    @Test
    public void test_scheduler_ticks() throws InterruptedException, ExecutionException, TimeoutException {
        final TestClock testClock = new TestClock();

        final TestExecutorService testExecutorService = new TestExecutorService(testClock);

        AtomicInteger count = new AtomicInteger(0);

        final QueryableScheduledFuture<?> scheduledFuture = testExecutorService.scheduleOnce(() -> {
            count.addAndGet(1);
        }, 10, TimeUnit.SECONDS);

        while(!scheduledFuture.isReady()){
            Thread.sleep(1);
        }

        testClock.tickSeconds(10L);

        scheduledFuture.get(3, TimeUnit.SECONDS);

        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    public void test_periodic_scheduler_runs() throws InterruptedException, ExecutionException {
        final TestClock testClock = new TestClock();

        final TestExecutorService testExecutorService = new TestExecutorService(testClock);

        AtomicInteger count = new AtomicInteger(0);

        final ScheduledFuture<?> scheduledFuture = testExecutorService.periodicWithDelay(() -> {
            count.addAndGet(1);

        }, 10, 10, TimeUnit.SECONDS);

        do {
            testClock.tickSeconds(1L);

            Thread.sleep(10);
        } while (count.get() != 3);

        final int current = count.get();

        scheduledFuture.cancel(false);

        // make sure its actually stopped
        for (int i = 0; i < 50; i++) {
            testClock.tickSeconds(20L);

            Thread.sleep(10);
        }

        assertThat(count.get()).isEqualTo(current).withFailMessage("Did not expected cycles");
    }
}
