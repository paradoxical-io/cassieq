package io.paradoxical.cassieq.admin.resources.api.v1;

import com.codahale.metrics.annotation.Timed;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.paradoxical.cassieq.discoverable.auth.SignedUrlParameterNames;
import io.paradoxical.cassieq.exceptions.AccountKeyNotFoundException;
import io.paradoxical.cassieq.exceptions.AccountNotFoundException;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.QueryAuthUrlResult;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.GetAuthQueryParamsRequest;
import io.paradoxical.cassieq.model.accounts.KeyName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.MacProviders;
import io.paradoxical.cassieq.model.auth.SignedUrlSignatureGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Path("/api/v1/permissions/")
@Api(value = "/api/v1/permissions/", description = "Permissions api", tags = "permissions")
@Produces(MediaType.APPLICATION_JSON)
public class PermissionsResource {

    private static final Logger logger = LoggerFactory.getLogger(PermissionsResource.class);
    private final DataContextFactory dataContextFactory;

    @Inject
    public PermissionsResource(
            DataContextFactory dataContextFactory) {
        this.dataContextFactory = dataContextFactory;
    }

    @GET
    @Path("/supportedAuthorizationLevels")
    @Timed
    @ApiOperation(value = "List available authorization levels")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response listAuthorizationLevels() {
        final List<Object> permissions =
                Arrays.stream(AuthorizationLevel.values())
                      .map(a -> new Object() {
                          public final String level = a.name();
                          public final String shortForm = a.getShortForm();
                      }).collect(toList());

        return Response.ok(permissions).build();
    }

    @POST
    @Timed
    @ApiOperation(value = "Generate auth url")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Ok"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response generateAuthUrl(@NotNull GetAuthQueryParamsRequest request) throws NoSuchAlgorithmException, InvalidKeyException {
        final Optional<AccountDefinition> accountDefinition = dataContextFactory.getAccountRepository().getAccount(request.getAccountName());

        if (!accountDefinition.isPresent()) {
            throw new AccountNotFoundException("CreateSignedAuthQuery", request.getAccountName());
        }

        final EnumSet<AuthorizationLevel> authorizationLevels = EnumSet.copyOf(request.getLevels());

        final ImmutableMap<KeyName, AccountKey> keys = accountDefinition.get().getKeys();

        if (!keys.containsKey(request.getKeyName())) {
            throw new AccountKeyNotFoundException("GenerateQueryAuth", request.getKeyName());
        }

        final SignedUrlSignatureGenerator signedUrlParameterGenerator =
                new SignedUrlSignatureGenerator(
                        request.getAccountName(),
                        authorizationLevels,
                        request.getStartTime(),
                        request.getEndTime(),
                        request.getQueueName());

        final AccountKey key = keys.get(request.getKeyName());

        final String queryParam = SignedUrlParameterNames.queryBuilder()
                                                         .fromSignatureGenerator(signedUrlParameterGenerator)
                                                         .build(MacProviders.HmacSha256(key));

        return Response.status(Response.Status.CREATED)
                       .entity(new QueryAuthUrlResult(queryParam))
                       .build();
    }
}
