package io.paradoxical.cassieq.discoverable.bundles;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.paradoxical.cassieq.metrics.QueueDelineatedMetricProvider;

@SuppressWarnings("unused")
public class QueueTimerBundle implements Bundle {
    @Override
    public void initialize(final Bootstrap<?> bootstrap) {

    }

    @Override
    public void run(final Environment environment) {
        environment.jersey().register(new QueueDelineatedMetricProvider(environment.metrics()));
    }
}

