package io.paradoxical.cassieq.admin.resources.api.v1;

import com.codahale.metrics.annotation.Timed;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.resources.api.BaseResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Path("/api/v1/accounts/")
@Api(value = "/api/v1/accounts/", description = "Account api", tags = "accounts")
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource extends BaseResource {

    private static final Logger logger = LoggerFactory.getLogger(AccountResource.class);
    private final DataContextFactory dataContextFactory;
    private final SecureRandom secureRandom;

    @Inject
    public AccountResource(
            DataContextFactory dataContextFactory,
            SecureRandom secureRandom) {
        this.dataContextFactory = dataContextFactory;
        this.secureRandom = secureRandom;
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

            if (accountCreated.isPresent()) {
                return Response.status(Response.Status.CREATED).entity(accountCreated.get()).build();
            }

            return Response.status(Response.Status.CONFLICT).build();
        }
        catch (Exception e) {
            logger.with(accountName).error(e, "Error creating account");

            return buildErrorResponse("CreateAccount", null, e);
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Get Accounts")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response getAccount() {

        try {
            final AccountRepository accountRepository = dataContextFactory.getAccountRepository();

            final List<AccountDefinition> accounts = accountRepository.getAllAccounts();

            return Response.ok(accounts).build();
        }
        catch (Exception e) {
            logger.error(e, "Error getting accounts");

            return buildErrorResponse("ListAccount", null, e);
        }
    }

    @GET
    @Timed
    @Path("/{accountName}")
    @ApiOperation(value = "Get Account")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response getAccount(
            @PathParam("accountName") AccountName accountName) {

        try {
            final AccountRepository accountRepository = dataContextFactory.getAccountRepository();

            final Optional<AccountDefinition> account = accountRepository.getAccount(accountName);

            if (!account.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(account.get()).build();
        }
        catch (Exception e) {
            logger.with("account-name", accountName).error(e, "Error getting account");

            return buildErrorResponse("GetAccount", null, e);
        }
    }

    @DELETE
    @Timed
    @Path("/{accountName}/keys/{keyName}")
    @ApiOperation(value = "Delete Account Key")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response deleteAccountKey(@PathParam("accountName") AccountName accountName, @PathParam("keyName") String keyName) {
        try {
            final AccountRepository accountRepository = dataContextFactory.getAccountRepository();

            final Optional<AccountDefinition> account = accountRepository.getAccount(accountName);

            if (!account.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            final AccountDefinition accountDefinition = account.get();

            final HashMap<String, AccountKey> prunedKeys = new HashMap<>(accountDefinition.getKeys());

            prunedKeys.remove(keyName);

            accountDefinition.setKeys(ImmutableMap.copyOf(prunedKeys));

            accountRepository.updateAccount(accountDefinition);

            return Response.ok(accountDefinition).build();

        }
        catch (Exception e) {
            logger.with("account-name", accountName)
                  .with("key-name", keyName)
                  .error(e, "Error getting account");

            return buildErrorResponse("RemoveAccountKey", null, e);
        }
    }

    @POST
    @Timed
    @Path("/{accountName}/keys/{keyName}")
    @ApiOperation(value = "Delete Account Key")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
                            @ApiResponse(code = 500, message = "Server Error") })
    @Consumes(MediaType.TEXT_PLAIN)
    public Response addNewKey(@PathParam("accountName") AccountName accountName,
                              @PathParam("keyName") String keyName,
                              @Nullable String base64KeyBody) {
        try {
            final AccountRepository accountRepository = dataContextFactory.getAccountRepository();

            final Optional<AccountDefinition> account = accountRepository.getAccount(accountName);

            if (!account.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            final AccountDefinition accountDefinition = account.get();

            if(accountDefinition.getKeys().containsKey(keyName)){
                return buildConflictResponse("Key " + keyName + " already exists");
            }

            final HashMap<String, AccountKey> prunedKeys = new HashMap<>(accountDefinition.getKeys());

            AccountKey key = Strings.isNullOrEmpty(base64KeyBody) ? AccountKey.random(secureRandom) : AccountKey.valueOf(base64KeyBody);

            prunedKeys.put(keyName, key);

            accountDefinition.setKeys(ImmutableMap.copyOf(prunedKeys));

            accountRepository.updateAccount(accountDefinition);

            return Response.ok().build();

        }
        catch (Exception e) {
            logger.with("account-name", accountName)
                  .with("key-name", keyName)
                  .error(e, "Error creating account key");

            return buildErrorResponse("AddAccountKey", null, e);
        }
    }


}
