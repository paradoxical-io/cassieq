package io.paradoxical.cassieq.resources.api.v1;

import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import lombok.Getter;
import org.glassfish.jersey.server.ManagedAsync;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;

@Path("/api/v1/ping")
@Api(value = "/api/v1/ping", description = "Ping api")
@Produces(MediaType.APPLICATION_JSON)
public class PingResource {

    private static final Logger logger = LoggerFactory.getLogger(PingResource.class);


    @Inject
    public PingResource() {
    }

    @GET
    @Path("/{echo}")
    @ApiOperation(value = "Ping")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    public Response echo(@PathParam("echo") String echo) {

        Object response = new Object() {
            @Getter

            String val = echo;
        };

        return Response.ok(response)
                       .status(Response.Status.OK)
                       .build();
    }

    @GET
    @ManagedAsync
    @Path("/async/{echo}")
    @ApiOperation(value = "Ping")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
    public void asyncEcho(@Suspended final AsyncResponse response, @PathParam("echo") String echo) throws ExecutionException, InterruptedException {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        logger.debug("Faking async call");
                        Thread.sleep(1000);
                    }
                    catch (Exception ex) {
                        logger.error(ex, "Async call threw an exception");
                    }

                    return echo;
                })
                .thenApply(reply -> {

                    final Response result = Response.ok().entity(new Object() {
                        public String val = reply;
                    }).build();

                    return response.resume(result);
                });
    }
}
