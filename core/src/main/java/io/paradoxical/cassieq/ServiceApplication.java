package io.paradoxical.cassieq;

import com.godaddy.logging.Logger;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewRenderer;
import io.dropwizard.views.mustache.MustacheViewRenderer;
import io.paradoxical.cassieq.bundles.DevGuiceBundleProvider;
import io.paradoxical.cassieq.bundles.GuiceBundleProvider;
import io.paradoxical.cassieq.commands.ConfigDumpCommand;
import io.paradoxical.cassieq.commands.DebugCommand;
import io.paradoxical.cassieq.commands.DevCommand;
import io.paradoxical.cassieq.commands.GenerateHttpsCertsCommand;
import io.paradoxical.cassieq.commands.HelpCommand;
import io.paradoxical.cassieq.commands.SetupDbCommand;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class ServiceApplication extends Application<ServiceConfiguration> {

    private static final Logger logger = getLogger(ServiceApplication.class);

    @Getter
    private final GuiceBundleProvider guiceBundleProvider;

    protected Environment env;

    public ServiceApplication(final GuiceBundleProvider guiceBundleProvider) {
        this.guiceBundleProvider = guiceBundleProvider;
    }

    public static void main(String[] args) throws Exception {
        DateTimeZone.setDefault(DateTimeZone.UTC);

        ServiceApplication application = null;

        if (!ArrayUtils.isEmpty(args)) {
            if (args[0].equals(DevCommand.ModeName)) {
                application = new ServiceApplication(new DevGuiceBundleProvider());
            }
        }

        if (application == null) {
            application = new ServiceApplication(new GuiceBundleProvider());
        }

        try {
            application.run(args);
        }
        catch (Throwable ex) {
            ex.printStackTrace();

            System.exit(1);
        }
    }

    @Override
    public void initialize(Bootstrap<ServiceConfiguration> bootstrap) {
        bootstrap.addCommand(new ConfigDumpCommand());
        bootstrap.addCommand(new DebugCommand());
        bootstrap.addCommand(new GenerateHttpsCertsCommand());
        bootstrap.addCommand(new HelpCommand());
        bootstrap.addCommand(new SetupDbCommand());
        bootstrap.addCommand(new DevCommand(this));

        bootstrap.addBundle(new TemplateConfigBundle());

        initializeViews(bootstrap);

        initializeDepedencyInjection(bootstrap);
    }

    private void initializeViews(final Bootstrap<ServiceConfiguration> bootstrap) {
        List<ViewRenderer> viewRenders = new ArrayList<>();

        viewRenders.add(new MustacheViewRenderer());

        bootstrap.addBundle(new ViewBundle<>(viewRenders));

        bootstrap.addBundle(new AssetsBundle("/assets", "/assets"));
    }

    private void initializeDepedencyInjection(final Bootstrap<ServiceConfiguration> bootstrap) {
        bootstrap.addBundle(guiceBundleProvider.getBundle());
    }

    @Override
    public void run(ServiceConfiguration config, final Environment env) throws Exception {
        this.env = env;

        new ServiceConfigurator(config, env).setup();
    }
}
