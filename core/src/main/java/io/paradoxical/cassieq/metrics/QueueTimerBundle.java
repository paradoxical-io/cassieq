package io.paradoxical.cassieq.metrics;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class QueueTimerBundle implements Bundle {
    @Override
    public void initialize(final Bootstrap<?> bootstrap) {

    }

    @Override
    public void run(final Environment environment) {
        environment.jersey().getResourceConfig().register(new QueueDelineatedMetricProvider(environment.metrics()));
    }
}

