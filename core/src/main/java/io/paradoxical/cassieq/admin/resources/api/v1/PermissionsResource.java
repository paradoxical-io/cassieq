package io.paradoxical.cassieq.admin.resources.api.v1;

import com.codahale.metrics.annotation.Timed;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.paradoxical.cassieq.discoverable.auth.SignedUrlParameterNames;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.QueryAuthUrlResult;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.GetAuthQueryParamsRequest;
import io.paradoxical.cassieq.model.accounts.KeyName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.SignedUrlParameterGenerator;
import io.paradoxical.cassieq.resources.api.BaseResource;
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
public class PermissionsResource extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(PermissionsResource.class);
    private final DataContextFactory dataContextFactory;

    @Inject
    public PermissionsResource(
            DataContextFactory dataContextFactory) {
        this.dataContextFactory = dataContextFactory;
    }

    @GET
    @Timed
    @ApiOperation(value = "List available permissions")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response listPermissions() {
        final List<Object> permissions = Arrays.stream(AuthorizationLevel.values()).map(a -> new Object() {
            public String level = a.name();
            public String shortForm = a.getShortForm();
        }).collect(toList());

        return Response.ok(permissions).build();
    }

    @POST
    @Timed
    @ApiOperation(value = "Generate auth url")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response generateAuthUrl(@NotNull GetAuthQueryParamsRequest request) throws NoSuchAlgorithmException, InvalidKeyException {
        final Optional<AccountDefinition> accountDefinition = dataContextFactory.getAccountRepository().getAccount(request.getAccountName());

        if (!accountDefinition.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final EnumSet<AuthorizationLevel> authorizationLevels = EnumSet.copyOf(request.getLevels());

        final ImmutableMap<KeyName, AccountKey> keys = accountDefinition.get().getKeys();

        if (!keys.containsKey(request.getKeyName())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final SignedUrlParameterGenerator signedUrlParameterGenerator = new SignedUrlParameterGenerator(request.getAccountName(), authorizationLevels);

        final String computedSignature = signedUrlParameterGenerator.computeSignature(keys.get(request.getKeyName()));

        final String queryParam = SignedUrlParameterNames.builder()
                                                         .auth(authorizationLevels)
                                                         .sig(computedSignature)
                                                         .build();

        return Response.ok(new QueryAuthUrlResult(queryParam)).build();
    }
}
