package io.paradoxical.cassieq.unittests.tests;

import io.paradoxical.cassieq.unittests.time.TestClock;
import io.paradoxical.cassieq.unittests.time.TestExecutorService;
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

@RunWith(Parameterized.class)

public class TimeTests {
    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[10][0]);
    }

    @Test
    public void test_scheduler_ticks() throws InterruptedException, ExecutionException, TimeoutException {
        final TestClock testClock = new TestClock();

        final TestExecutorService testExecutorService = new TestExecutorService(testClock);

        AtomicInteger count = new AtomicInteger(0);

        final ScheduledFuture<?> scheduledFuture = testExecutorService.scheduleOnce(() -> {
            count.addAndGet(1);
        }, 10, TimeUnit.SECONDS);

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

        testClock.tickSeconds(10L);

        Thread.sleep(50);

        testClock.tickSeconds(10L);

        Thread.sleep(50);

        int current = count.get();

        assertThat(current).isGreaterThanOrEqualTo(1);

        testClock.tickSeconds(10L);

        Thread.sleep(50);

        testClock.tickSeconds(10L);

        Thread.sleep(50);

        current = count.get();

        assertThat(current).isGreaterThanOrEqualTo(2);

        scheduledFuture.cancel(true);

        current = count.get();

        assertThat(count.get()).isBetween(current, current + 1).withFailMessage("Did not expected cycles");
    }
}
