package io.paradoxical.cassieq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewRenderer;
import io.dropwizard.views.mustache.MustacheViewRenderer;
import io.paradoxical.cassieq.auth.AccountPrincipal;
import io.paradoxical.cassieq.auth.AuthToken;
import io.paradoxical.cassieq.auth.SignedRequestAuthFilter;
import io.paradoxical.cassieq.bundles.GuiceBundleProvider;
import io.paradoxical.cassieq.configurations.LogMapping;
import io.paradoxical.cassieq.serialization.JacksonJsonMapper;
import io.paradoxical.common.web.web.filter.CorrelationIdFilter;
import io.paradoxical.common.web.web.filter.JerseyRequestLogging;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.jersey.listing.ApiListingResourceJSON;
import lombok.Getter;
import org.joda.time.DateTimeZone;

import javax.ws.rs.core.Response;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static com.godaddy.logging.LoggerFactory.getLogger;


public class ServiceApplication extends Application<ServiceConfiguration> {

    private static final Logger logger = getLogger(ServiceApplication.class);

    @Getter
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

        run.add(this::configureSwaggerApi);

        run.add(this::configureLogging);

        run.add(this::configureAuth);

        run.stream().forEach(configFunction -> configFunction.accept(config, env));
    }

    private void configureAuth(final ServiceConfiguration serviceConfiguration, final Environment environment) {

        final Injector injector = getGuiceBundleProvider().getBundle().getInjector();

        final Key<Authenticator<AuthToken, AccountPrincipal>> authenticatorKey = Key.get(new TypeLiteral<Authenticator<AuthToken, AccountPrincipal>>() {});

        final SignedRequestAuthFilter<AccountPrincipal> authFilter =
                SignedRequestAuthFilter.<AccountPrincipal>builder()
                        .accountNamePathParameter("accountName")
                        .setAuthenticator(injector.getInstance(authenticatorKey))
                        .setPrefix("Signed")
                        .setAuthorizer((principal, role) -> true)
                        .setUnauthorizedHandler((prefix, realm) -> Response.status(Response.Status.UNAUTHORIZED).build())
                        .buildAuthFilter();


        environment.jersey().register(authFilter);
    }

    private void configureFilters(final ServiceConfiguration serviceConfiguration, final Environment environment) {
        environment.jersey().register(new CorrelationIdFilter());

        if (serviceConfiguration.getLogConfig().getLogRawJerseyRequests()) {
            environment.jersey().register(new JerseyRequestLogging());
        }
    }

    private void configureLogging(final ServiceConfiguration serviceConfiguration, final Environment environment) {
        LogMapping.register();
    }

    private void configureSwaggerApi(
            final ServiceConfiguration config,
            final Environment environment) {

        final BeanConfig swagConfig = new BeanConfig();
        swagConfig.setScan(true);
        swagConfig.setTitle("cassieq");
        swagConfig.setDescription("cassieq API");
        swagConfig.setLicense("Apache 2.0");
        swagConfig.setResourcePackage("io.paradoxical.cassieq");
        swagConfig.setLicenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html");


        swagConfig.setVersion("1.0.1");
        swagConfig.setBasePath(environment.getApplicationContext().getContextPath());

        environment.jersey().register(new ApiListingResourceJSON());
        environment.jersey().register(new SwaggerSerializers());

    }

    protected void configureJson(ServiceConfiguration config, final Environment environment) {
        ObjectMapper mapper = new JacksonJsonMapper().getMapper();

        JacksonMessageBodyProvider jacksonBodyProvider = new JacksonMessageBodyProvider(mapper, environment.getValidator());

        environment.jersey().register(jacksonBodyProvider);
    }
}
