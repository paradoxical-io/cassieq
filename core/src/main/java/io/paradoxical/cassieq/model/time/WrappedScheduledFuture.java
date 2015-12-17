package io.paradoxical.cassieq.model.time;

import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WrappedScheduledFuture<T> implements QueryableScheduledFuture<T> {
    private final ScheduledFuture<T> wrapped;

    public WrappedScheduledFuture(ScheduledFuture<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean isReady() {
        return !wrapped.isCancelled() && !wrapped.isDone();
    }

    public long getDelay(final TimeUnit unit) {return this.wrapped.getDelay(unit);}

    public T get() throws InterruptedException, java.util.concurrent.ExecutionException {return this.wrapped.get();}

    public boolean isCancelled() {return this.wrapped.isCancelled();}

    public int compareTo(final Delayed o) {return this.wrapped.compareTo(o);}

    public T get(final long timeout, final TimeUnit unit)
            throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {return this.wrapped.get(timeout, unit);}

    public boolean cancel(final boolean mayInterruptIfRunning) {return this.wrapped.cancel(mayInterruptIfRunning);}

    public boolean isDone() {return this.wrapped.isDone();}
}
