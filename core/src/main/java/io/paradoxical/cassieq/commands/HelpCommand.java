package io.paradoxical.cassieq.commands;

import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import lombok.Cleanup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.BufferedReader;
import java.io.FileReader;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Provides usage information");
    }

    @Override
    public void configure(final Subparser subparser) {
    }

    @Override
    public void run(final Bootstrap<?> bootstrap, final Namespace namespace) throws Exception {
        final String helpTextPath = "/data/content/help.txt";

        @Cleanup final FileReader fileReader = new FileReader(helpTextPath);

        new BufferedReader(fileReader).lines().forEach(System.out::println);
        }
}
