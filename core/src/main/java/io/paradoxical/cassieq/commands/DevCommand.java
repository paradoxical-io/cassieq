package io.paradoxical.cassieq.commands;

import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.cli.ServerCommand;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.paradoxical.cassieq.ServiceApplication;
import io.paradoxical.cassieq.ServiceConfiguration;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class DevCommand extends EnvironmentCommand<ServiceConfiguration> {
    public static final String ModeName = "dev";

    private final ServerCommand<ServiceConfiguration> serverCommand;

    public DevCommand(ServiceApplication application) {
        super(application,
              ModeName,
              "Start a local embedded cassandra to experiment with the api. Fully encapsulated with no dependencies!");
        serverCommand = new ServerCommand<>(application);
    }

    @Override
    public void run(final Bootstrap<?> wildcardBootstrap, final Namespace namespace) throws Exception {
        serverCommand.run(wildcardBootstrap, namespace);
    }


    @Override
    public void configure(final Subparser subparser) {
        ConfigDumpCommand.setupDefaultConfigFile(subparser);
    }

    @Override
    protected void run(final Environment environment, final Namespace namespace, final ServiceConfiguration configuration) throws Exception {

    }
}
