package io.paradoxical.cassieq.unittests.time;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import io.paradoxical.cassieq.model.time.Clock;
import io.paradoxical.cassieq.model.time.QueryableScheduledFuture;
import io.paradoxical.cassieq.model.time.Scheduler;
import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static com.godaddy.logging.LoggerFactory.getLogger;

class ClosedClockExecutor implements Scheduler {
    private static final Logger logger = getLogger(ClosedClockExecutor.class);

    private final Clock clock;
    private Instant start;

    @Inject
    public ClosedClockExecutor(Clock clock, final Instant start) {
        this.clock = clock;
        this.start = start;
    }


    @Override
    public QueryableScheduledFuture<?> periodicWithDelay(final Runnable runnable, final long delay, final long duration, final TimeUnit unit) {
        final QueryableScheduledFuture<?> scheduledFuture = scheduleOnce(runnable, duration, unit);

        final Boolean[] isCancelled = { false };
        final Boolean[] isReady = { false };

        final Thread loopedScheduler = new Thread(() -> {
            while (!isCancelled[0]) {
                try {
                    final Instant stop = start.plus(duration);

                    if (clock.now().isBefore(stop)) {
                        final long millis = unit.toMillis(duration);

                        isReady[0] = true;

                        clock.sleepFor(Duration.millis(millis));

                        isReady[0] = false;
                    }

                    start = clock.now();
                }
                catch (InterruptedException e) {
                    logger.error(e, "Error waiting for duration");
                }

                if (!isCancelled[0]) {
                    runnable.run();
                }
            }
        });

        final CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
            try {
                scheduledFuture.get();
            }
            catch (InterruptedException | ExecutionException e) {
                logger.error(e, "Error");

                throw new RuntimeException(e);
            }
        }).thenAcceptAsync(v -> {
            try {
                loopedScheduler.start();

                loopedScheduler.join();
            }
            catch (Exception ignored) {
            }
        });

        return scheduledFutureFromCompletble(voidCompletableFuture, () -> isCancelled[0] = true, () -> scheduledFuture.isReady() || isReady[0]);
    }

    @Override
    public QueryableScheduledFuture<?> scheduleOnce(final Runnable runnable, final long duration, final TimeUnit unit) {
        final Boolean[] isReady = { false };

        final CompletableFuture<Void> onceSchedule = CompletableFuture.runAsync(() -> {
            try {
                final long millis = unit.toMillis(duration);

                final Instant stop = start.plus(duration);

                if (clock.now().isBefore(stop)) {
                    isReady[0] = true;

                    clock.sleepFor(Duration.millis(millis));
                }

                runnable.run();
            }
            catch (InterruptedException e) {
                logger.error(e, "Error waiting for duration");
            }
        });

        return scheduledFutureFromCompletble(onceSchedule,
                                             () -> {},
                                             () -> isReady[0]);
    }

    private QueryableScheduledFuture<?> scheduledFutureFromCompletble(final CompletableFuture<Void> voidCompletableFuture, Runnable onCancel, Supplier<Boolean> isReady) {
        return new QueryableScheduledFuture<Object>() {
            @Override
            public boolean isReady() {
                return isReady.get();
            }

            @Override
            public long getDelay(final TimeUnit unit) {
                return 0;
            }

            @Override
            public int compareTo(final Delayed o) {
                return o.compareTo(this);
            }

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                onCancel.run();

                try {
                    return voidCompletableFuture.cancel(mayInterruptIfRunning);
                }
                catch (Throwable ignored) {
                    return true;
                }
            }

            @Override
            public boolean isCancelled() {
                return voidCompletableFuture.isCancelled();
            }

            @Override
            public boolean isDone() {
                return voidCompletableFuture.isDone();
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                return voidCompletableFuture.get();
            }

            @Override
            public Object get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return voidCompletableFuture.get(timeout, unit);
            }
        };
    }
}

public class TestExecutorService implements Scheduler {

    private static final Logger logger = getLogger(TestExecutorService.class);

    private final Clock clock;

    @Inject
    public TestExecutorService(Clock clock) {
        this.clock = clock;
    }


    @Override
    public QueryableScheduledFuture<?> periodicWithDelay(final Runnable runnable, final long delay, final long duration, final TimeUnit unit) {
        return new ClosedClockExecutor(clock, clock.now()).periodicWithDelay(runnable, delay, duration, unit);
    }

    @Override
    public QueryableScheduledFuture<?> scheduleOnce(final Runnable runnableFuture, final long duration, final TimeUnit unit) {
        return new ClosedClockExecutor(clock, clock.now()).scheduleOnce(runnableFuture, duration, unit);
    }
}
