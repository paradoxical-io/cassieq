package io.paradoxical.cassieq.admin.resources;

import io.dropwizard.views.View;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class AdminPagesResource {

    @GET
    public IndexView handleSwagger2() {
        return new IndexView("/swagger2.mustache");
    }

    public static class IndexView extends View {
        protected IndexView(String templateName) {
            super(templateName);
        }
    }
}