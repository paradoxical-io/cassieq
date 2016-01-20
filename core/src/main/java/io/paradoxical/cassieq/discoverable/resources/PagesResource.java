package io.paradoxical.cassieq.discoverable.resources;

import io.dropwizard.views.View;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class PagesResource {

    @GET
    public IndexView handleSwagger() {
        return new IndexView("/swagger2.mustache");
    }

    public static class IndexView extends View {
        protected IndexView(String templateName) {
            super(templateName);
        }
    }
}