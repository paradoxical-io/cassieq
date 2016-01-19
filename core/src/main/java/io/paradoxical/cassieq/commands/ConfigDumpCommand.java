package io.paradoxical.cassieq.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.dropwizard.cli.Command;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.setup.Bootstrap;
import io.paradoxical.cassieq.ServiceConfiguration;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class ConfigDumpCommand extends Command {

    public ConfigDumpCommand() {
        super("config", "Prints out default config settings for use in creating custom config files");
    }

    @Override
    public void configure(final Subparser subparser) {
        subparser.addArgument("-a", "--app-config")
                 .dest("app")
                 .type(String.class)
                 .help("Print app yaml");
    }

    @Override
    public void run(final Bootstrap<?> bootstrap, final Namespace namespace) throws Exception {
        final Bootstrap<ServiceConfiguration> serviceConfigurationBootstrap = (Bootstrap<ServiceConfiguration>) bootstrap;

        final String configPath = namespace.getString("app");

        if (configPath != null) {
            printDefaultAppValues(configPath, serviceConfigurationBootstrap);
        }
    }

    private void printDefaultAppValues(final String configPath, final Bootstrap<ServiceConfiguration> bootstrap) throws IOException, ConfigurationException {
        final ConfigurationFactoryFactory<ServiceConfiguration> configurationFactoryFactory = bootstrap.getConfigurationFactoryFactory();

        final ConfigurationFactory<ServiceConfiguration> configurationFactory =
                configurationFactoryFactory.create(ServiceConfiguration.class,
                                                   bootstrap.getValidatorFactory().getValidator(),
                                                   bootstrap.getObjectMapper(),
                                                   StringUtils.EMPTY);

        final ServiceConfiguration config = configurationFactory.build(bootstrap.getConfigurationSourceProvider(), configPath);

        final YAMLFactory yamlFactory = new YAMLFactory();

        yamlFactory.configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false);

        final ObjectMapper objectMapper = new ObjectMapper(yamlFactory);

        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        System.out.println(objectMapper.writeValueAsString(config));
    }
}
