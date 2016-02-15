package io.paradoxical.cassieq.modules;

import io.paradoxical.cassieq.model.time.Clock;
import io.paradoxical.cassieq.model.time.JodaSleepableClock;
import com.google.inject.AbstractModule;
import io.paradoxical.cassieq.model.time.SleepableClock;

public class ClockModule extends AbstractModule {
    @Override protected void configure() {
        bind(Clock.class).to(JodaSleepableClock.class);
        bind(SleepableClock.class).to(JodaSleepableClock.class);
    }
}
