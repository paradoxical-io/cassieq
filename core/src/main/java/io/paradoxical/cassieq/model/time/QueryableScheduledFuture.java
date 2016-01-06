package io.paradoxical.cassieq.model.time;

import lombok.experimental.Delegate;

import java.util.concurrent.ScheduledFuture;

public interface QueryableScheduledFuture<T> extends ScheduledFuture<T> {
    boolean isReady();
}

