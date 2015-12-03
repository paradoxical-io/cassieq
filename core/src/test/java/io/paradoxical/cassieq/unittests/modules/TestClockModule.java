package io.paradoxical.cassieq.unittests.modules;

import io.paradoxical.cassieq.model.Clock;
import io.paradoxical.cassieq.modules.ClockModule;
import io.paradoxical.cassieq.unittests.time.TestClock;
import io.paradoxical.common.test.guice.OverridableModule;
import com.google.inject.Module;

public class TestClockModule extends OverridableModule {

    private final TestClock clock;

    public TestClockModule(TestClock clock) {
        this.clock = clock;
    }

    @Override
    public Class<? extends Module> getOverridesModule() {
        return ClockModule.class;
    }

    @Override
    protected void configure() {
        bind(Clock.class).toInstance(clock);
    }
}
