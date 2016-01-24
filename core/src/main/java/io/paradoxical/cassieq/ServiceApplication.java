package io.paradoxical.cassieq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.google.common.base.Charsets;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.servlets.assets.AssetServlet;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.ViewRenderer;
import io.dropwizard.views.mustache.MustacheViewRenderer;
import io.paradoxical.cassieq.admin.AdminRoot;
import io.paradoxical.cassieq.admin.resources.AdminPagesResource;
import io.paradoxical.cassieq.admin.resources.api.v1.AccountResource;
import io.paradoxical.cassieq.admin.resources.api.v1.PermissionsResource;
import io.paradoxical.cassieq.auth.AuthLevelDynamicFeature;
import io.paradoxical.cassieq.bundles.GuiceBundleProvider;
import io.paradoxical.cassieq.commands.ConfigDumpCommand;
import io.paradoxical.cassieq.configurations.LogMapping;
import io.paradoxical.cassieq.discoverable.ApiDiscoverableRoot;
import io.paradoxical.cassieq.serialization.JacksonJsonMapper;
import io.paradoxical.cassieq.swagger.SwaggerApiResource;
import io.paradoxical.common.web.web.filter.CorrelationIdFilter;
import io.paradoxical.common.web.web.filter.JerseyRequestLogging;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import lombok.Getter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.joda.time.DateTimeZone;

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
        bootstrap.addCommand(new ConfigDumpCommand());

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

        ArrayList<BiConsumer<ServiceConfiguration, Environment>> run = new ArrayList<>();

        run.add(this::configureJson);

        run.add(this::configureFilters);

        run.add(this::configureSwaggerApi);

        run.add(this::configureLogging);

        run.add(this::configureAuth);

        run.add(this::configureAdmin);

        run.stream().forEach(configFunction -> configFunction.accept(config, env));
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

    private void configureAdmin(final ServiceConfiguration serviceConfiguration, final Environment environment) {
        environment.admin().addServlet("assets-admin", new AssetServlet("/assets", "/assets", null, Charsets.UTF_8)).addMapping("/assets/*");

        final DropwizardResourceConfig adminResourceConfig = new DropwizardResourceConfig(environment.metrics());
        JerseyContainerHolder adminContainerHolder = new JerseyContainerHolder(new ServletContainer(adminResourceConfig));

        adminResourceConfig.register(AdminPagesResource.class);
        adminResourceConfig.register(PermissionsResource.class);
        adminResourceConfig.register(AccountResource.class);

        adminResourceConfig.register(new SwaggerSerializers());

        configureAdminSwagger(adminResourceConfig);

        List<ViewRenderer> viewRenders = new ArrayList<>();

        viewRenders.add(new MustacheViewRenderer());

        final ViewMessageBodyWriter viewMessageBodyWriter = new ViewMessageBodyWriter(environment.metrics(), viewRenders);

        adminResourceConfig.register(viewMessageBodyWriter);

        environment.admin().addServlet("admin-resources", adminContainerHolder.getContainer()).addMapping("/admin/*");
    }

    private void configureAdminSwagger(DropwizardResourceConfig resourceConfig) {

        final BeanConfig swagConfig = new BeanConfig();
        swagConfig.setTitle("cassieq admin");
        swagConfig.setDescription("cassieq admin API");
        swagConfig.setLicense("Apache 2.0");
        swagConfig.setResourcePackage(AdminRoot.packageName());
        swagConfig.setLicenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html");
        swagConfig.setVersion("0.9");

        swagConfig.setBasePath("/admin");

        resourceConfig.register(new SwaggerApiResource(swagConfig));
    }

    private void configureAuth(final ServiceConfiguration serviceConfiguration, final Environment environment) {
        environment.jersey().register(AuthLevelDynamicFeature.class);
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
        swagConfig.setResourcePackage(ApiDiscoverableRoot.packageName());
        swagConfig.setLicenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html");
        swagConfig.setVersion("0.9");
        swagConfig.setBasePath(environment.getApplicationContext().getContextPath());

        environment.jersey().register(new SwaggerSerializers());

        environment.jersey().register(new SwaggerApiResource(swagConfig));
    }

    protected void configureJson(ServiceConfiguration config, final Environment environment) {
        ObjectMapper mapper = new JacksonJsonMapper().getMapper();

        JacksonMessageBodyProvider jacksonBodyProvider = new JacksonMessageBodyProvider(mapper, environment.getValidator());

        environment.jersey().register(jacksonBodyProvider);
    }
}
