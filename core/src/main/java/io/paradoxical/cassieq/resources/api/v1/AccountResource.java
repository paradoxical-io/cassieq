package io.paradoxical.cassieq.resources.api.v1;

import com.codahale.metrics.annotation.Timed;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.inject.Inject;
import com.hazelcast.client.impl.client.BaseClientRemoveListenerRequest;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("/api/v1/accounts/")
@Api(value = "/api/v1/accounts/", description = "Account api", tags = "accounts")
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(AccountResource.class);
    private final DataContextFactory dataContextFactory;

    @Inject
    public AccountResource(DataContextFactory dataContextFactory) {
        this.dataContextFactory = dataContextFactory;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create Account")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Created"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response createAccount(AccountName accountName) {

        try {
            final AccountRepository accountRepository = dataContextFactory.getAccountRepository();

            final Optional<AccountDefinition> accountCreated = accountRepository.createAccount(accountName);

            if(accountCreated.isPresent()){
                return Response.status(Response.Status.CREATED).entity(accountCreated.get()).build();
            }

            return Response.status(Response.Status.CONFLICT).build();
        }
        catch (Exception e) {
            logger.error(e, "Error");
            return buildErrorResponse("CreateQueue", null, e);
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Create Account")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Created"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response getAccount(AccountName accountName) {

        try {

            return Response.status(Response.Status.CONFLICT).build();
        }
        catch (Exception e) {
            logger.error(e, "Error");
            return buildErrorResponse("CreateQueue", null, e);
        }
    }
}
