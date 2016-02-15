package io.paradoxical.cassieq.commands;

import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import lombok.Cleanup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.BufferedReader;
import java.io.File;
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
        final File helpFile = new File(helpTextPath);

        final CharSource helpTextSource = Files.asCharSource(helpFile, Charsets.UTF_8);

        helpTextSource.copyTo(System.out);
    }
}
