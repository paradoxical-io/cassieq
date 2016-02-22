package io.paradoxical.cassieq.commands;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import io.paradoxical.cassieq.environment.SystemProps;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.File;

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
        final File helpFile = new File(helpTextPath);

        final CharSource helpTextSource = Files.asCharSource(helpFile, Charsets.UTF_8);

        helpTextSource.copyTo(System.out);

        final String pattern = "%-25s %-30s %-30s %s\n";

        System.out.printf(String.format(pattern, "EnvVar", "CurrentValue", "DefaultValue", "HelpText"));

        SystemProps.discover().forEach(property -> {
            System.out.printf(String.format(pattern,
                                            property.getEnvVarName(),
                                            property.getCurrentValue(),
                                            property.getDefaultValue(),
                                            property.getHelp()

            ));
        });
    }
}
