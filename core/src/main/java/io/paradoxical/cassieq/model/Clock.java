package io.paradoxical.cassieq.model;

import org.joda.time.Duration;
import org.joda.time.Instant;

public interface Clock {
    Instant now();

    void sleepFor(Duration duration) throws InterruptedException;
}

