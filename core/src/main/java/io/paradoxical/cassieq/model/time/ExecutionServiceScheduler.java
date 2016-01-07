package io.paradoxical.cassieq.model.time;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExecutionServiceScheduler implements Scheduler {
    final ScheduledExecutorService scheduledExecutorService;

    public ExecutionServiceScheduler(int periodicThreadPoolSize) {
        scheduledExecutorService = Executors.newScheduledThreadPool(periodicThreadPoolSize);
    }

    @Override
    public ScheduledFuture<?> periodicWithDelay(final Runnable runnable, final long delay, final long duration, final TimeUnit unit) {
        return scheduledExecutorService.scheduleWithFixedDelay(runnable, delay, duration, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleOnce(final Runnable runnable, final long duration, final TimeUnit unit) {
        return scheduledExecutorService.schedule(runnable, duration, unit);
    }
}
