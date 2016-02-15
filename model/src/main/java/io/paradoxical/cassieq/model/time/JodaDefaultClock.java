package io.paradoxical.cassieq.model.time;

import org.joda.time.Instant;

public class JodaDefaultClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
