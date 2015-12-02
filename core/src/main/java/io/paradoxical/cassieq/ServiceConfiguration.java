package io.paradoxical.cassieq;

import io.paradoxical.cassieq.configurations.JerseyConfiguration;
import io.dropwizard.Configuration;
import lombok.Setter;

public class ServiceConfiguration extends Configuration {

    public JerseyConfiguration getJerseyConfiguration() {
        if (jerseyConfiguration == null) {
            return new JerseyConfiguration();
        }

        return jerseyConfiguration;
    }

    @Setter
    private JerseyConfiguration jerseyConfiguration;
}
