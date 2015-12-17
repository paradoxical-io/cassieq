package io.paradoxical.cassieq.model.time;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface Scheduler {
    /**
     * Schedule with a delay. Wait for the runnable to complete, then wait the duration and schedule again
     *
     * @param runnable
     * @param delay
     * @param duration
     * @param unit
     * @return
     */
    QueryableScheduledFuture<?> periodicWithDelay(Runnable runnable, long delay, long duration, TimeUnit unit);

    QueryableScheduledFuture<?> scheduleOnce(Runnable runnableFuture, long duration, TimeUnit unit);
}
