package io.paradoxical.cassieq.unittests;

import categories.BuildVerification;
import com.squareup.okhttp.ResponseBody;
import io.paradoxical.cassieq.api.client.CassieqApi;
import io.paradoxical.cassieq.api.client.CassieqCredentials;
import io.paradoxical.cassieq.model.QueryAuthUrlResult;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.accounts.GetAuthQueryParamsRequest;
import io.paradoxical.cassieq.model.accounts.KeyName;
import io.paradoxical.cassieq.model.accounts.WellKnownKeyNames;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.time.Clock;
import io.paradoxical.cassieq.unittests.server.AdminClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit.Response;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class ApiAuthenticationTests extends ApiTestsBase {

    /**
     * Correct permissions and correct authorization level
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    @Test
    public void test_query_auth_authorizes() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        final AccountName accountName = AccountName.valueOf("test_query_auth_authorizes");

        final Response<AccountDefinition> createAccountResponse = adminClient.createAccount(accountName).execute();

        assertThat(createAccountResponse.isSuccess()).isTrue();

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest =
                GetAuthQueryParamsRequest.builder()
                                         .accountName(accountName)
                                         .keyName(KeyName.valueOf(WellKnownKeyNames.Primary.getKeyName()))
                                         .level(AuthorizationLevel.CreateQueue)
                                         .build();

        final QueryAuthUrlResult result = adminClient.createPermissions(getAuthQueryParamsRequest).execute().body();

        final String queryParam = result.getQueryParam();

        final CassieqApi client = CassieqApi.createClient(server.getBaseUri().toString(), CassieqCredentials.signedQueryString(queryParam));

        final Response<ResponseBody> authAuthorizes = client.createQueue(accountName, new QueueCreateOptions(QueueName.valueOf("test_query_auth_authorizes")))
                                                            .execute();

        assertThat(authAuthorizes.isSuccess()).isTrue();
    }

    /**
     * Valid signature, but not correct permissions
     *
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     */
    @Test
    public void test_query_auth_authenticates() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        final AccountName accountName = AccountName.valueOf("test_query_auth_authenticates");

        adminClient.createAccount(accountName).execute();

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest =
                GetAuthQueryParamsRequest.builder()
                                         .accountName(accountName)
                                         .keyName(WellKnownKeyNames.Primary)
                                         .level(AuthorizationLevel.ReadMessage)
                                         .build();

        final QueryAuthUrlResult result = adminClient.createPermissions(getAuthQueryParamsRequest).execute().body();

        final String queryParam = result.getQueryParam();

        final CassieqApi client = CassieqApi.createClient(server.getBaseUri().toString(), CassieqCredentials.signedQueryString(queryParam));

        final Response<ResponseBody> authAuthorizes = client.createQueue(accountName, new QueueCreateOptions(QueueName.valueOf("test_query_auth_authenticates")))
                                                            .execute();

        assertThat(authAuthorizes.isSuccess()).isFalse();

        // unauthorized
        assertThat(authAuthorizes.code()).isEqualTo(403);
    }

    @Test
    public void when_making_requests_outside_the_allowed_skew_they_should_be_unauthed() throws Exception {
        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        class DelayedClock implements Clock {

            @Override
            public Instant now() {
                return getTestClock().now().minus(Duration.standardHours(1));
            }
        }

        final AccountName accountName = AccountName.valueOf("when_making_requests_outside_the_allowed_skew_they_should_be_unauthed");
        final QueueName queueName = QueueName.valueOf("when_making_requests_outside_the_allowed_skew_they_should_be_unauthed");

        final Response<AccountDefinition> createAccount = adminClient.createAccount(accountName).execute();
        final AccountKey accountKey = createAccount.body().getKeys().get(WellKnownKeyNames.Primary.getKeyName());

        final CassieqApi delayedClient =
                CassieqApi.createClient(server.getBaseUri(),
                                        CassieqCredentials.key(accountName, accountKey, new DelayedClock()));

        final Response<ResponseBody> createRequest =
                delayedClient.createQueue(accountName, new QueueCreateOptions(queueName)).execute();

        assertThat(createRequest.isSuccess()).isFalse();
        assertThat(createRequest.code()).isEqualTo(401);

        final CassieqApi fullAccessClient =
                CassieqApi.createClient(server.getBaseUri(),
                                        CassieqCredentials.key(accountName, accountKey));

        final Response<ResponseBody> createWithoutDelayResponse =
                fullAccessClient.createQueue(accountName, new QueueCreateOptions(queueName)).execute();

        assertThat(createWithoutDelayResponse.isSuccess()).isTrue();
    }

    @Test
    public void test_query_auth_restricts_queue() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        final AccountName accountName = AccountName.valueOf("test_query_auth_restricts_queue");
        final QueueName queueName = QueueName.valueOf("test_query_auth_restricts_queue");
        final QueueName badQueueName = QueueName.valueOf(queueName.get() + "bad");

        final Response<AccountDefinition> createAccount = adminClient.createAccount(accountName).execute();
        final AccountKey accountKey = createAccount.body().getKeys().get(WellKnownKeyNames.Primary.getKeyName());

        final CassieqApi fullAccessClient = CassieqApi.createClient(server.getBaseUri(), CassieqCredentials.key(accountName, accountKey));

        // make both queues exist
        final Response<ResponseBody> goodQueueCreateRequest = fullAccessClient.createQueue(accountName, new QueueCreateOptions(queueName)).execute();
        final Response<ResponseBody> badQueueCreateRequest = fullAccessClient.createQueue(accountName, new QueueCreateOptions(badQueueName)).execute();

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest =
                GetAuthQueryParamsRequest.builder()
                                         .accountName(accountName)
                                         .keyName(WellKnownKeyNames.Primary)
                                         .level(AuthorizationLevel.PutMessage)
                                         .queueName(queueName)
                                         .build();

        final QueryAuthUrlResult result = adminClient.createPermissions(getAuthQueryParamsRequest).execute().body();

        final String queryParam = result.getQueryParam();

        final CassieqApi authedClient =
                CassieqApi.createClient(server.getBaseUri(),
                                        CassieqCredentials.signedQueryString(queryParam));

        final Response<ResponseBody> goodRequestIsAuthorized =
                authedClient.addMessage(accountName, queueName, "Hello queue").execute();

        assertThat(goodRequestIsAuthorized.isSuccess()).isTrue();

        final Response<ResponseBody> badRequestAuthorizes =
                authedClient.addMessage(accountName, badQueueName, "Hello bad queue").execute();

        assertThat(badRequestAuthorizes.isSuccess()).isFalse();
        assertThat(badRequestAuthorizes.code()).isEqualTo(401);
    }

    @Test
    public void test_query_auth_authenticates_with_end_time() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        final AccountName accountName = AccountName.valueOf("test_query_auth_authenticates_with_end_time");

        final QueueName queueName = QueueName.valueOf("test_query_auth_authenticates_with_end_time");

        adminClient.createAccount(accountName).execute();

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest =
                GetAuthQueryParamsRequest.builder()
                                         .accountName(accountName)
                                         .keyName(WellKnownKeyNames.Primary)
                                         .level(AuthorizationLevel.CreateQueue)
                                         .endTime(DateTime.now(DateTimeZone.UTC).plus(Period.days(5)))
                                         .build();

        final QueryAuthUrlResult result = adminClient.createPermissions(getAuthQueryParamsRequest).execute().body();

        final String queryParam = result.getQueryParam();

        final CassieqApi client = CassieqApi.createClient(server.getBaseUri().toString(), CassieqCredentials.signedQueryString(queryParam));

        final Response<ResponseBody> authAuthorizes = client.createQueue(accountName, new QueueCreateOptions(queueName))
                                                            .execute();

        assertThat(authAuthorizes.isSuccess()).isTrue();
    }

    @Test
    public void test_invalid_key_name_fails() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        final AccountName accountName = AccountName.valueOf("test_invalid_key_name_fails");

        adminClient.createAccount(accountName).execute();

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest =
                GetAuthQueryParamsRequest.builder()
                                         .accountName(accountName)
                                         .keyName(KeyName.valueOf("invalid!"))
                                         .level(AuthorizationLevel.CreateQueue)
                                         .endTime(DateTime.now(DateTimeZone.UTC).minus(Period.seconds(3)))
                                         .build();

        final Boolean result = adminClient.createPermissions(getAuthQueryParamsRequest).execute().isSuccess();

        assertThat(result).isFalse();
    }

    @Test
    public void test_query_auth_expires_with_end_time() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        final AccountName accountName = AccountName.valueOf("test_query_auth_expires_with_end_time");

        final QueueName queueName = QueueName.valueOf("test_query_auth_expires_with_end_time");

        adminClient.createAccount(accountName).execute();

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest =
                GetAuthQueryParamsRequest.builder()
                                         .accountName(accountName)
                                         .keyName(WellKnownKeyNames.Primary)
                                         .level(AuthorizationLevel.CreateQueue)
                                         .endTime(DateTime.now(DateTimeZone.UTC).minus(Period.seconds(3)))
                                         .build();

        final QueryAuthUrlResult result = adminClient.createPermissions(getAuthQueryParamsRequest).execute().body();

        final String queryParam = result.getQueryParam();

        final CassieqApi client = CassieqApi.createClient(server.getBaseUri().toString(), CassieqCredentials.signedQueryString(queryParam));

        final Response<ResponseBody> authAuthorizes = client.createQueue(accountName, new QueueCreateOptions(queueName))
                                                            .execute();

        assertThat(authAuthorizes.isSuccess()).isFalse();
        assertThat(authAuthorizes.code()).isEqualTo(401);
    }

    /**
     * Invalid signature
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    @Test
    public void test_query_auth_prevents_invalid_authentiation() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        final AccountName accountName = AccountName.valueOf("test_query_auth_prevents_invalid_authentiation");

        adminClient.createAccount(accountName).execute();

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest =
                GetAuthQueryParamsRequest.builder()
                                         .accountName(accountName)
                                         .keyName(WellKnownKeyNames.Primary)
                                         .level(AuthorizationLevel.CreateQueue)
                                         .build();

        final QueryAuthUrlResult result = adminClient.createPermissions(getAuthQueryParamsRequest).execute().body();

        final String queryParam = result.getQueryParam();

        final CassieqApi client = CassieqApi.createClient(server.getBaseUri().toString(), CassieqCredentials.signedQueryString(queryParam + "fail"));

        final QueueName queueName = QueueName.valueOf("test_query_auth_prevents_invalid_authentiation");
        final Response<ResponseBody> authAuthorizes = client.createQueue(accountName,
                                                                         new QueueCreateOptions(queueName))
                                                            .execute();

        assertThat(authAuthorizes.isSuccess()).isFalse();

        // unauthenticated
        assertThat(authAuthorizes.code()).isEqualTo(401);
    }

    @Test
    public void test_revoked_key_prevents_authentiation() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        final AccountName accountName = AccountName.valueOf("test_revoked_key_prevents_authentiation");

        adminClient.createAccount(accountName).execute();

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest =
                GetAuthQueryParamsRequest.builder()
                                         .accountName(accountName)
                                         .keyName(WellKnownKeyNames.Primary)
                                         .level(AuthorizationLevel.CreateQueue)
                                         .build();

        final QueryAuthUrlResult result = adminClient.createPermissions(getAuthQueryParamsRequest).execute().body();

        final String queryParam = result.getQueryParam();

        final CassieqApi client = CassieqApi.createClient(server.getBaseUri().toString(), CassieqCredentials.signedQueryString(queryParam));

        final Response<ResponseBody> authAuthorizes = client.createQueue(accountName,
                                                                         new QueueCreateOptions(QueueName.valueOf("test_revoked_key_prevents_authentiation")))
                                                            .execute();

        assertThat(authAuthorizes.isSuccess()).isTrue();

        adminClient.deleteAccountKey(accountName, WellKnownKeyNames.Primary.getKeyName()).execute();

        final Response<ResponseBody> usingOldCreds = client.createQueue(accountName,
                                                                        new QueueCreateOptions(QueueName.valueOf("test_revoked_key_prevents_authentiation_failure")))
                                                           .execute();

        // unauthenticated since key was revoked
        assertThat(usingOldCreds.code()).isEqualTo(401);
    }
}
