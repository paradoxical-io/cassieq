package io.paradoxical.cassieq.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.dropwizard.cli.Command;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.paradoxical.cassieq.ServiceConfiguration;
import lombok.Cleanup;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;

public class ConfigDumpCommand extends ConfiguredCommand<ServiceConfiguration> {

    private static final String DefaultConfigFile = "/data/conf/configuration.yml";

    public ConfigDumpCommand() {
        super("config", "Prints out the config settings for use in creating custom config files");
    }

    @Override
    public void run(final Bootstrap<?> wildcardBootstrap, final Namespace namespace) throws Exception {
        wildcardBootstrap.setConfigurationFactoryFactory(
                (klass, validator, objectMapper, propertyPrefix) ->
                        new ConfigurationFactory<>(klass, null, objectMapper, propertyPrefix));

        super.run(wildcardBootstrap, namespace);
    }

    @Override
    public void configure(final Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("-f", "--full")
                .action(Arguments.storeTrue())
                .dest("full")
                .help("Dumps the full configuration object as YAML");
    }

    @Override
    protected void run(final Bootstrap<ServiceConfiguration> bootstrap, final Namespace namespace, final ServiceConfiguration configuration) throws Exception {

        final String configFile = namespace.getString("file");

        if (namespace.getBoolean("full")) {
            final YAMLFactory yamlFactory = new YAMLFactory();

            yamlFactory.configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false);

            final ObjectMapper objectMapper = new ObjectMapper(yamlFactory);

            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

            System.out.println(objectMapper.writeValueAsString(configuration));

            return;
        }

        @Cleanup final FileReader fileReader = new FileReader(configFile == null ? DefaultConfigFile : configFile);

        new BufferedReader(fileReader).lines().forEach(System.out::println);

        System.out.println();
    }
}
