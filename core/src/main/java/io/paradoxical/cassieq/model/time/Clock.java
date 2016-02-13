package io.paradoxical.cassieq.model.time;

import org.joda.time.Instant;

public interface Clock {
    Instant now();
}

