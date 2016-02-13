package io.paradoxical.cassieq.commands;

import com.google.common.base.Joiner;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.File;
import java.io.IOException;

public class GenerateHttpsCertsCommand extends Command {
    public GenerateHttpsCertsCommand() {
        super("gen-https", "Generates Self-Signed https certificates, useful for getting HTTPS setup easily. " +
                           "Writes output to /data/https so make sure that volume is mounted");
    }

    @Override
    public void configure(final Subparser subparser) {
        subparser.addArgument("password")
                 .dest("password")
                 .type(String.class)
                 .help("the PKCS12 certificate store password")
                 .required(true);

        subparser.addArgument("certName")
                 .dest("certName")
                 .type(String.class)
                 .help("the generated certificates common name (CN)")
                 .required(true);

        subparser.addArgument("days")
                 .dest("vaildDays")
                 .type(Integer.class)
                 .help("the number of days the certificate should be valid for (Default: 30)")
                 .setDefault(30)
                 .required(false);
    }

    @Override
    public void run(final Bootstrap<?> bootstrap, final Namespace namespace) throws Exception {
        final File outputDirectory = new File("/data/https");

        System.out.println("Generating serverKeys.p12 into /data/https (make sure you have it mounted)");

        buildCert(namespace, outputDirectory);

        buildP12(namespace, outputDirectory);

    }

    private void buildCert(final Namespace namespace, final File outputDirectory) throws InterruptedException, IOException {
        final String certName = String.format("/O=Paradoxical.io/OU=Generated-Certificates/CN=%s", namespace.getString("certName"));

        final Integer vaildDays = namespace.getInt("vaildDays");

        ProcessBuilder processBuilder =
                new ProcessBuilder("openssl",
                                   "req",
                                   "-x509",
                                   "-newkey", "2048",
                                   "-keyout", "private_key.pem",
                                   "-out", "cert.pem",
                                   "-days", vaildDays.toString(),
                                   "-nodes",
                                   "-subj", certName)
                        .inheritIO()
                        .directory(outputDirectory);


        final int result = processBuilder
                .start()
                .waitFor();

        if (result != 0) {
            System.out.println("oops something went wrong...");
        }
        else {
            System.out.println("Successfully generated certificate...");
            System.out.println();
        }
    }

    private void buildP12(final Namespace namespace, final File outputDirectory) throws InterruptedException, IOException {
        final String certName = namespace.getString("certName");

        System.out.println("Adding generated certificate to PKCS12 file (/data/https/serverKeys.p12)");

        ProcessBuilder processBuilder =
                new ProcessBuilder("openssl",
                                   "pkcs12",
                                   "-export",
                                   "-out", "serverKeys.p12",
                                   "-password", "pass:" + namespace.getString("password"),
                                   "-in", "cert.pem",
                                   "-inkey", "private_key.pem",
                                   "-name", certName)
                        .inheritIO()
                        .directory(outputDirectory);

        final Process process = processBuilder.start();

        final int result = process
                .waitFor();

        if (result != 0) {
            System.out.println("oops something went wrong generating p12...");
        }
        else{
            System.out.println("serverKeys.p12 successfully generated into /data/https");
        }
    }
}
