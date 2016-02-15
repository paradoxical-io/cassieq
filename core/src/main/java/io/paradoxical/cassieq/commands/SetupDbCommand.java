package io.paradoxical.cassieq.commands;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.paradoxical.cassandra.loader.DbRunnerConfig;
import io.paradoxical.cassandra.loader.DbScriptsRunner;
import io.paradoxical.cassieq.ServiceConfiguration;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.util.Scanner;

public class SetupDbCommand extends ConfiguredCommand<ServiceConfiguration> {

    public SetupDbCommand() {
        super("setup-db", "Initializes the cassandra database");
    }

    @Override
    public void configure(final Subparser subparser) {
        subparser.addArgument("--config")
                 .dest("file")
                 .nargs("?")
                 .setDefault("/data/conf/configuration.yml")
                 .help("application configuration file");

        subparser.addArgument("-i", "--ip")
                 .dest("ip")
                 .help("Cassandra cluster IP");

        subparser.addArgument("-u", "--userName")
                 .dest("userName")
                 .help("Cassandra DB user name");

        subparser.addArgument("-p", "--password")
                 .dest("password")
                 .help("Cassandra DB password");

        subparser.addArgument("-c", "--createKeyspace")
                 .dest("shouldCreateKeyspace")
                 .action(Arguments.storeFalse())
                 .setDefault(true)
                 .help("Use to disable keyspace creation");

        subparser.addArgument("-k", "--keyspace")
                 .dest("keyspace")
                 .help("the keyspace to create");

        subparser.addArgument("--port")
                 .dest("port")
                 .setDefault(9042)
                 .type(Integer.class)
                 .help("The port to connect to cassandra on");

        subparser.addArgument("-v", "--databaseVersino")
                 .dest("databaseVersino")
                 .type(Integer.class)
                 .help("The database version");

        subparser.addArgument("--recreate")
                .dest("recreateKeyspace")
                .action(Arguments.storeTrue())
                .help("weather or not to recreate the keyspace");

    }

    @Override
    protected void run(final Bootstrap<ServiceConfiguration> bootstrap, final Namespace namespace, final ServiceConfiguration configuration) throws Exception {
        DbRunnerConfig dbRunnerConfig = getDbRunnerConfig(namespace);

        if (dbRunnerConfig.getRecreateDatabase()) {
            recreateDatabase(dbRunnerConfig);
        }

        DbScriptsRunner dbScriptsRunner = new DbScriptsRunner(dbRunnerConfig);

        dbScriptsRunner.run();
    }

    private DbRunnerConfig getDbRunnerConfig(final Namespace namespace) {
        DbRunnerConfig.DbRunnerConfigBuilder dbConfigBuilder = DbRunnerConfig.builder();

        String username = namespace.getString("userName");
        String password = namespace.getString("pw");

        dbConfigBuilder.ip(namespace.getString("ip"))
                       .port(namespace.getInt("port"))
                       .username(username != null ? username : "")
                       .password(password != null ? password : "")
                       .createKeyspace(namespace.getBoolean("shouldCreateKeyspace"))
                       .keyspace(namespace.getString("keyspace"))
//                       .dbVersion(namespace.getInt("databaseVersion"))
                       .filePath("/data/db")
                       .recreateDatabase(namespace.getBoolean("recreateKeyspace"));

        return dbConfigBuilder.build();
    }

    private static void recreateDatabase(DbRunnerConfig dbRunnerConfig) {
        Scanner reader = new Scanner(System.in);
        System.out.println("Are you sure you want to recreate the database? This will delete everything from the " + dbRunnerConfig.getKeyspace() + " keyspace. (y/n)");

        if ("y".equalsIgnoreCase(reader.nextLine())) {
            System.out.println("Enter the name of the keyspace you wish to recreate");

            String enteredKeyspace = reader.nextLine();

            if (!dbRunnerConfig.getKeyspace().equals(enteredKeyspace)) {
                System.out.println("Keyspace did not match with keyspace entered in flag");
                System.exit(0);
            }

            dbRunnerConfig.setKeyspace(enteredKeyspace);
        }
        else {
            System.out.println("Database will not be recreated.");
            System.exit(0);
        }
    }
}
