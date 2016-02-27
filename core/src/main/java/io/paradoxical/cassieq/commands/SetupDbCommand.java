package io.paradoxical.cassieq.commands;

import com.godaddy.logging.Logger;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import io.paradoxical.cassandra.loader.DbRunnerConfig;
import io.paradoxical.cassandra.loader.DbScriptsRunner;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.util.Scanner;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class SetupDbCommand extends Command {

    private static final Logger logger = getLogger(SetupDbCommand.class);

    public SetupDbCommand() {
        super("setup-db", "Initializes the cassandra database");
    }

    @Override
    public void configure(final Subparser subparser) {

        subparser.addArgument("-i", "--ip")
                 .dest("ip")
                 .required(true)
                 .help("Cassandra cluster IP");

        subparser.addArgument("-u", "--userName")
                 .dest("userName")
                 .setDefault("guest")
                 .help("Cassandra DB user name");

        subparser.addArgument("-p", "--password")
                 .dest("password")
                 .setDefault("guest")
                 .help("Cassandra DB password");

        subparser.addArgument("--dontCreateKeyspace")
                 .dest("shouldCreateKeyspace")
                 .action(Arguments.storeFalse())
                 .setDefault(true)
                 .help("Use to disable keyspace creation");

        subparser.addArgument("-k", "--keyspace")
                 .dest("keyspace")
                 .required(true)
                 .help("the keyspace to create");

        subparser.addArgument("--port")
                 .dest("port")
                 .setDefault(9042)
                 .type(Integer.class)
                 .help("The port to connect to cassandra on");

        subparser.addArgument("--recreate")
                 .dest("recreateKeyspace")
                 .action(Arguments.storeTrue())
                 .help("Whether or not to recreate the keyspace");
    }

    @Override
    public void run(final Bootstrap<?> bootstrap, final Namespace namespace) throws Exception {
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
