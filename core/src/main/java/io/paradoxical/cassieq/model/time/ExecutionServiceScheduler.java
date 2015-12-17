package io.paradoxical.cassieq.model.time;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutionServiceScheduler implements Scheduler {
    final ScheduledExecutorService scheduledExecutorService;

    public ExecutionServiceScheduler(int periodicThreadPoolSize) {
        scheduledExecutorService = Executors.newScheduledThreadPool(periodicThreadPoolSize);
    }

    @Override
    public QueryableScheduledFuture<?> periodicWithDelay(final Runnable runnable, final long delay, final long duration, final TimeUnit unit) {
        return new WrappedScheduledFuture<>(scheduledExecutorService.scheduleWithFixedDelay(runnable, delay, duration, unit));
    }

    @Override
    public QueryableScheduledFuture<?> scheduleOnce(final Runnable runnable, final long duration, final TimeUnit unit) {
        return new WrappedScheduledFuture<>(scheduledExecutorService.schedule(runnable, duration, unit));
    }
}
