package io.paradoxical.cassieq.commands;

import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.File;

public class DebugCommand extends Command {
    public DebugCommand() {
        super("debug", "Runs bash so that you can explore the container");
    }

    @Override
    public void configure(final Subparser subparser) {
    }

    @Override
    public void run(final Bootstrap<?> bootstrap, final Namespace namespace) throws Exception {
        new ProcessBuilder("/bin/bash")
                .directory(new File("/data"))
                .inheritIO()
                .start()
                .waitFor();
    }
}
