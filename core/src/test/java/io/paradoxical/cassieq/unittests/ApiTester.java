package io.paradoxical.cassieq.unittests;

import categories.BuildVerification;
import categories.VerySlowTests;
import com.godaddy.logging.Logger;
import com.google.inject.Injector;
import com.squareup.okhttp.ResponseBody;
import io.paradoxical.cassieq.admin.resources.api.v1.AccountResource;
import io.paradoxical.cassieq.admin.resources.api.v1.PermissionsResource;
import io.paradoxical.cassieq.api.client.CassieqApi;
import io.paradoxical.cassieq.api.client.CassieqCredentials;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueryAuthUrlResult;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.UpdateMessageRequest;
import io.paradoxical.cassieq.model.UpdateMessageResponse;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.accounts.GetAuthQueryParamsRequest;
import io.paradoxical.cassieq.model.accounts.KeyName;
import io.paradoxical.cassieq.model.accounts.WellKnownKeyNames;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.server.SelfHostServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit.Response;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class ApiTester extends DbTestBase {

    private static final Logger logger = getLogger(ApiTester.class);

    private static SelfHostServer server;

    private static CassieqApi client;

    public ApiTester() throws NoSuchAlgorithmException, InvalidKeyException {

    }

    @BeforeClass
    public static void setup() throws InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        server = new SelfHostServer(new InMemorySessionProvider(session));

        server.start();

        server.getService().waitForRun();

        client = CassieqApi.createClient(server.getBaseUri().toString(),
                                         getTestAccountCredentials(server.getService().getGuiceBundleProvider().getInjector()));
    }

    @AfterClass
    public static void cleanup() {
        server.stop();
    }

    @Test
    public void put_into_deleted_queue_fails() throws IOException {
        final QueueName queueName = QueueName.valueOf("put_into_deleted_queue_fails");

        client.createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        client.deleteQueue(testAccountName, queueName).execute();

        final Response<ResponseBody> result = client.addMessage(testAccountName, queueName, "foo").execute();

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void test_client_can_create_put_and_ack() throws Exception {
        final QueueName queueName = QueueName.valueOf("test_client_can_create_put_and_ack");

        client.createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        client.addMessage(testAccountName, queueName, "hi").execute();

        getTestClock().tick();

        final Response<GetMessageResponse> message = client.getMessage(testAccountName, queueName).execute();

        final GetMessageResponse body = message.body();

        assertThat(body).isNotNull();

        final String popReceipt = body.getPopReceipt();

        assertThat(popReceipt).isNotNull();

        final Response<ResponseBody> ackResponse = client.ackMessage(testAccountName, queueName, popReceipt).execute();

        assertThat(ackResponse.isSuccess()).isTrue();
    }

    /**
     * Correct permissions and correct authorization level
     *
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    @Test
    public void test_query_auth_authorizes() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        final Injector injector = server.getService().getGuiceBundleProvider().getInjector();

        final AccountResource accountResource = injector.getInstance(AccountResource.class);

        final PermissionsResource permissionsResource = injector.getInstance(PermissionsResource.class);

        final AccountName accountName = AccountName.valueOf("test_query_auth_authorizes");

        accountResource.createAccount(accountName);

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest = new GetAuthQueryParamsRequest(accountName,
                                                                                                  KeyName.valueOf(WellKnownKeyNames.Primary.getKeyName()),
                                                                                                  Collections.singletonList(AuthorizationLevel.CreateQueue));

        final QueryAuthUrlResult result = (QueryAuthUrlResult) permissionsResource.generateAuthUrl(getAuthQueryParamsRequest)
                                                                                  .getEntity();

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
        final Injector injector = server.getService().getGuiceBundleProvider().getInjector();

        final AccountResource accountResource = injector.getInstance(AccountResource.class);

        final PermissionsResource permissionsResource = injector.getInstance(PermissionsResource.class);

        final AccountName accountName = AccountName.valueOf("test_query_auth_authenticates");

        accountResource.createAccount(accountName);

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest = new GetAuthQueryParamsRequest(accountName,
                                                                                                  KeyName.valueOf(WellKnownKeyNames.Primary.getKeyName()),
                                                                                                  Collections.singletonList(AuthorizationLevel.ReadMessage));

        final QueryAuthUrlResult result = (QueryAuthUrlResult) permissionsResource.generateAuthUrl(getAuthQueryParamsRequest).getEntity();

        final String queryParam = result.getQueryParam();

        final CassieqApi client = CassieqApi.createClient(server.getBaseUri().toString(), CassieqCredentials.signedQueryString(queryParam));

        final Response<ResponseBody> authAuthorizes = client.createQueue(accountName, new QueueCreateOptions(QueueName.valueOf("test_query_auth_authenticates")))
                                                            .execute();

        assertThat(authAuthorizes.isSuccess()).isFalse();

        // unauthorized
        assertThat(authAuthorizes.code()).isEqualTo(403);
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
        final Injector injector = server.getService().getGuiceBundleProvider().getInjector();

        final AccountResource accountResource = injector.getInstance(AccountResource.class);

        final PermissionsResource permissionsResource = injector.getInstance(PermissionsResource.class);

        final AccountName accountName = AccountName.valueOf("test_query_auth_prevents_invalid_authentiation");

        accountResource.createAccount(accountName);

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest = new GetAuthQueryParamsRequest(accountName,
                                                                                                  KeyName.valueOf(WellKnownKeyNames.Primary.getKeyName()),
                                                                                                  Collections.singletonList(AuthorizationLevel.CreateQueue));

        final QueryAuthUrlResult result = (QueryAuthUrlResult) permissionsResource.generateAuthUrl(getAuthQueryParamsRequest).getEntity();

        final String queryParam = result.getQueryParam();

        final CassieqApi client = CassieqApi.createClient(server.getBaseUri().toString(), CassieqCredentials.signedQueryString(queryParam + "fail"));

        final Response<ResponseBody> authAuthorizes = client.createQueue(accountName,
                                                                         new QueueCreateOptions(QueueName.valueOf("test_query_auth_prevents_invalid_authentiation")))
                                                            .execute();

        assertThat(authAuthorizes.isSuccess()).isFalse();

        // unauthenticated
        assertThat(authAuthorizes.code()).isEqualTo(401);
    }

    @Test
    public void test_revoked_key_prevents_authentiation() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        final Injector injector = server.getService().getGuiceBundleProvider().getInjector();

        final AccountResource accountResource = injector.getInstance(AccountResource.class);

        final PermissionsResource permissionsResource = injector.getInstance(PermissionsResource.class);

        final AccountName accountName = AccountName.valueOf("test_revoked_key_prevents_authentiation");

        accountResource.createAccount(accountName);

        final GetAuthQueryParamsRequest getAuthQueryParamsRequest = new GetAuthQueryParamsRequest(accountName,
                                                                                                  KeyName.valueOf(WellKnownKeyNames.Primary.getKeyName()),
                                                                                                  Collections.singletonList(AuthorizationLevel.CreateQueue));

        final QueryAuthUrlResult result = (QueryAuthUrlResult) permissionsResource.generateAuthUrl(getAuthQueryParamsRequest).getEntity();

        final String queryParam = result.getQueryParam();

        final CassieqApi client = CassieqApi.createClient(server.getBaseUri().toString(), CassieqCredentials.signedQueryString(queryParam));

        final Response<ResponseBody> authAuthorizes = client.createQueue(accountName,
                                                                         new QueueCreateOptions(QueueName.valueOf("test_revoked_key_prevents_authentiation")))
                                                            .execute();

        assertThat(authAuthorizes.isSuccess()).isTrue();

        accountResource.deleteAccountKey(accountName, WellKnownKeyNames.Primary.getKeyName());


        final Response<ResponseBody> usingOldCreds = client.createQueue(accountName,
                                                                        new QueueCreateOptions(QueueName.valueOf("test_revoked_key_prevents_authentiation_failure")))
                                                           .execute();

        // unauthenticated since key was revoked
        assertThat(usingOldCreds.code()).isEqualTo(401);
    }


    @Test
    public void demo_invis_client() throws Exception {
        final QueueName queueName = QueueName.valueOf("demo_invis_client");

        client.createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        int count = 21;

        for (int i = 0; i < count; i++) {
            client.addMessage(testAccountName, queueName, Integer.valueOf(i).toString()).execute();
        }

        int c = -1;

        while (true) {
            c++;
            final Response<GetMessageResponse> message = client.getMessage(testAccountName, queueName, 1L).execute();

            final GetMessageResponse body = message.body();

            if (body == null) {
                break;
            }

            assertThat(body).isNotNull();

            final String popReceipt = body.getPopReceipt();

            System.out.println(String.format("Message id: %s, Delivery count %s", body.getMessage(), body.getDeliveryCount()));

            if (c == 0 || c == 10) {
                // message times out
                System.out.println("WAIT");
                Thread.sleep(2000);
                continue;
            }
            else {
                assertThat(popReceipt).isNotNull();

                final Response<ResponseBody> ackResponse = client.ackMessage(testAccountName, queueName, popReceipt).execute();

                System.out.println("ACK");

                assertThat(ackResponse.isSuccess()).isTrue();
            }
        }
    }

    @Test
    public void delete_queue() throws IOException {
        final QueueName delete_queue = QueueName.valueOf("delete_queue");

        client.createQueue(testAccountName, new QueueCreateOptions(delete_queue)).execute();

        assertThat(client.deleteQueue(testAccountName, delete_queue).execute().isSuccess()).isTrue();

        assertThat(client.getMessage(testAccountName, delete_queue).execute().isSuccess()).isFalse();
    }

    @Test
    public void update_message() throws Exception {
        final QueueName delete_queue = QueueName.valueOf("update_message");

        client.createQueue(testAccountName, new QueueCreateOptions(delete_queue)).execute();


        client.addMessage(testAccountName, delete_queue, "foo").execute();

        final GetMessageResponse body = client.getMessage(testAccountName, delete_queue).execute().body();

        final UpdateMessageResponse updateResponse =
                client.updateMessage(
                        testAccountName,
                        delete_queue,
                        body.getPopReceipt(),
                        new UpdateMessageRequest("foo2", 10L)).execute()
                      .body();

        client.ackMessage(testAccountName, delete_queue, updateResponse.getPopReceipt()).execute();

        assertThat(client.getMessage(testAccountName, delete_queue).execute().code()).isEqualTo(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test(timeout = 30000)
    @Category(VerySlowTests.class)
    public void test_invis_like_crazy() throws Exception {
        final QueueName queueName = QueueName.valueOf("test");

        client.createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        int count = 100;

        for (int i = 0; i < count; i++) {
            client.addMessage(testAccountName, queueName, Integer.valueOf(i).toString()).execute();
        }

        GetMessageResponse body;
        int i = 0;

        while ((body = getMessage(client, queueName)) != null && count > 0) {

            final String popReceipt = body.getPopReceipt();

            System.out.println(String.format("Message id: %s, Delivery count %s", body.getMessage(), body.getDeliveryCount()));

            if (i++ % 20 == 0 && body.getDeliveryCount() < 4) {
                // message times out
                Thread.sleep(3000);
            }
            else {
                assertThat(popReceipt).isNotNull();

                final Response<ResponseBody> ackResponse = client.ackMessage(testAccountName, queueName, popReceipt).execute();

                assertThat(ackResponse.isSuccess()).isTrue();

                count--;
            }

        }
    }

    private GetMessageResponse getMessage(final CassieqApi client, final QueueName queueName) throws java.io.IOException {
        final Response<GetMessageResponse> message = client.getMessage(testAccountName, queueName, 3L).execute();

        return message.body();
    }

}
