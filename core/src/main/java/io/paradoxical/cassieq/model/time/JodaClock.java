package io.paradoxical.cassieq.model.time;

import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.Random;

public final class JodaClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public void sleepFor(final Duration duration) throws InterruptedException {
        Thread.sleep(duration.getStandardSeconds() * 1000L);
    }

    @Override
    public long jitter(final int i) {
        return new Random().nextInt(i);
    }
}
