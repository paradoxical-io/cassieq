package io.paradoxical.cassieq.model.time;

import org.joda.time.Duration;
import org.joda.time.Instant;

public interface SleepableClock extends Clock {
    void sleepFor(Duration duration) throws InterruptedException;

    void sleepTill(Instant instant) throws InterruptedException;

    long jitter(int maximumJitter);
}
