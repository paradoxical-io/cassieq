package io.paradoxical.cassieq.modules;

import io.paradoxical.cassieq.model.time.Clock;
import io.paradoxical.cassieq.model.time.JodaClock;
import com.google.inject.AbstractModule;

public class ClockModule extends AbstractModule {
    @Override protected void configure() {
        bind(Clock.class).to(JodaClock.class);
    }
}
