package io.paradoxical.cassieq.model.time;

import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.Random;

public final class JodaSleepableClock extends JodaDefaultClock implements Clock, SleepableClock {

    @Override
    public void sleepFor(final Duration duration) throws InterruptedException {
        Thread.sleep(duration.getStandardSeconds() * 1000L);
    }

    @Override
    public void sleepTill(final Instant instant) throws InterruptedException {
        Thread.sleep(instant.minus(Instant.now().getMillis()).getMillis());
    }

    @Override
    public long jitter(final int maximumJitter) {
        return new Random().nextInt(maximumJitter);
    }
}
