package io.paradoxical.cassieq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.jersey.listing.ApiListingResourceJSON;
import com.wordnik.swagger.model.ApiInfo;
import com.wordnik.swagger.reader.ClassReaders;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewRenderer;
import io.dropwizard.views.mustache.MustacheViewRenderer;
import io.paradoxical.cassieq.bundles.GuiceBundleProvider;
import io.paradoxical.cassieq.serialization.JacksonJsonMapper;
import io.paradoxical.common.web.web.filter.CorrelationIdFilter;
import io.paradoxical.common.web.web.filter.JerseyRequestLogging;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static com.godaddy.logging.LoggerFactory.getLogger;


public class ServiceApplication extends Application<ServiceConfiguration> {

    private static final Logger logger = getLogger(ServiceApplication.class);
    private final GuiceBundleProvider guiceBundleProvider;
    private Environment env;

    public ServiceApplication(final GuiceBundleProvider guiceBundleProvider) {

        this.guiceBundleProvider = guiceBundleProvider;
    }

    public static void main(String[] args) throws Exception {
        DateTimeZone.setDefault(DateTimeZone.UTC);

        ServiceApplication serviceApplication = new ServiceApplication(new GuiceBundleProvider());

        try {
            serviceApplication.run(args);
        }
        catch (Throwable ex) {
            ex.printStackTrace();

            System.exit(1);
        }
    }

    @Override
    public void initialize(Bootstrap<ServiceConfiguration> bootstrap) {
        bootstrap.addBundle(new TemplateConfigBundle());

        initializeViews(bootstrap);

        initializeDepedencyInjection(bootstrap);

    }

    public void stop() {
        try {
            logger.info("Stopping!");

            env.getApplicationContext().getServer().stop();

            logger.info("Stopped");
        }
        catch (Exception ex) {
            logger.error(ex, "Unclean stop occurred!");
        }
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
        ArrayList<BiConsumer<ServiceConfiguration, Environment>> run = new ArrayList<>();

        run.add(this::configureJson);

        run.add(this::configureFilters);

        run.add(this::configureDiscoverableApiHelp);

        run.add(this::configureLogging);

        run.stream().forEach(configFunction -> configFunction.accept(config, env));
    }

    private void configureFilters(final ServiceConfiguration serviceConfiguration, final Environment environment) {
        environment.jersey().register(new CorrelationIdFilter());

        if (serviceConfiguration.getLogConfig().getLogRawJerseyRequests()) {
            environment.jersey().register(new JerseyRequestLogging());
        }
    }

    private void configureLogging(final ServiceConfiguration serviceConfiguration, final Environment environment) {
    }

    private void configureDiscoverableApiHelp(
            final ServiceConfiguration config,
            final Environment environment) {

        environment.jersey().register(new ApiListingResourceJSON());
        environment.jersey().register(new ResourceListingProvider());
        environment.jersey().register(new ApiDeclarationProvider());

        ScannerFactory.setScanner(new DefaultJaxrsScanner());

        ClassReaders.setReader(new DefaultJaxrsApiReader());

        SwaggerConfig swagConfig = ConfigFactory.config();

        swagConfig.setApiVersion("1.0.1");

        swagConfig.setBasePath(environment.getApplicationContext().getContextPath());

        ApiInfo info = new ApiInfo(
                "cassieq API",                             /* title */
                "cassieq API",
                "http://",                  /* TOS URL */
                "admin@cassieq.com",                            /* Contact */
                "Apache 2.0",                                     /* license */
                "http://www.apache.org/licenses/LICENSE-2.0.html" /* license URL */
        );

        swagConfig.setApiInfo(info);
    }

    protected void configureJson(ServiceConfiguration config, final Environment environment) {
        ObjectMapper mapper = new JacksonJsonMapper().getMapper();

        JacksonMessageBodyProvider jacksonBodyProvider = new JacksonMessageBodyProvider(mapper, environment.getValidator());

        environment.jersey().register(jacksonBodyProvider);
    }
}
