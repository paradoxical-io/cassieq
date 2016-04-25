package io.paradoxical.cassieq.unittests.tests.api;

import categories.BuildVerification;
import com.godaddy.logging.Logger;
import com.squareup.okhttp.ResponseBody;
import io.paradoxical.cassieq.api.client.CassieqApi;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.UpdateMessageRequest;
import io.paradoxical.cassieq.model.UpdateMessageResponse;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.unittests.server.AdminClient;
import io.paradoxical.cassieq.unittests.time.TestClock;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit.Response;

import java.io.IOException;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class StandardApiTests extends ApiTestsBase {

    private static final Logger logger = getLogger(StandardApiTests.class);

    @Test
    public void put_into_deleted_queue_fails() throws IOException {
        final QueueName queueName = QueueName.valueOf("put_into_deleted_queue_fails");

        apiClient().createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        apiClient().deleteQueue(testAccountName, queueName).execute();

        final Response<ResponseBody> result = apiClient().addMessage(testAccountName, queueName, "foo").execute();

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void initial_invis_respected() throws IOException {
        final QueueName queueName = QueueName.valueOf("initial_invis_respected");

        final CassieqApi client = apiClient();

        client.createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        client.addMessage(testAccountName, queueName, "0").execute();
        client.addMessage(testAccountName, queueName, "4", 400000L).execute();
        client.addMessage(testAccountName, queueName, "3", 300000L).execute();
        client.addMessage(testAccountName, queueName, "2", 200000L).execute();
        client.addMessage(testAccountName, queueName, "1").execute();

        int counter = 0;

        for (int i = 0; i < 5; ++i) {
            final Response<GetMessageResponse> body = client.getMessage(testAccountName, queueName, 10000L).execute();

            assertThat(body.isSuccess()).isTrue();

            counter += body.body() == null ? 0 : 1;
        }

        client.deleteQueue(testAccountName, queueName).execute();

        assertThat(counter)
                .isEqualTo(2)
                .withFailMessage("Initial messages did not respect invis time");
    }


    @Test
    public void create_queue_with_invalid_name_fails() throws IOException {
        final QueueName queueName = QueueName.valueOf("invalid!");

        final Response<ResponseBody> execute = apiClient().createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        assertThat(execute.isSuccess()).isFalse();
    }

    @Test
    public void create_queue_with_dots_works() throws IOException {
        final QueueName queueName = QueueName.valueOf("create.queue.with.dots.works");

        final Response<ResponseBody> execute = apiClient().createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        assertThat(execute.isSuccess()).isTrue();
    }

    @Test
    public void create_account_with_dots_works() throws IOException {
        final AccountName accountName = AccountName.valueOf("create.account.with.dots.works");

        final AdminClient adminClient = AdminClient.createClient(server.getAdminuri().toString());

        final Response<AccountDefinition> execute = adminClient.createAccount(accountName).execute();

        assertThat(execute.isSuccess()).isTrue();
    }

    @Test
    public void test_client_can_create_put_and_ack() throws Exception {
        final QueueName queueName = QueueName.valueOf("test_client_can_create_put_and_ack");

        apiClient().createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        apiClient().addMessage(testAccountName, queueName, "hi").execute();

        getTestClock().tick();

        final Response<GetMessageResponse> message = apiClient().getMessage(testAccountName, queueName).execute();

        final GetMessageResponse body = message.body();

        assertThat(body).isNotNull();

        final String popReceipt = body.getPopReceipt();

        assertThat(popReceipt).isNotNull();

        final Response<ResponseBody> ackResponse = apiClient().ackMessage(testAccountName, queueName, popReceipt).execute();

        assertThat(ackResponse.isSuccess()).isTrue();
    }

    @Test
    public void demo_invis_client() throws Exception {
        final QueueName queueName = QueueName.valueOf("demo_invis_client");

        apiClient().createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        int count = 50;

        for (int i = 0; i < count; i++) {
            apiClient().addMessage(testAccountName, queueName, Integer.valueOf(i).toString()).execute();
        }

        final TestClock clock = getTestClock();

        int c = -1;

        while (true) {
            c++;
            final Response<GetMessageResponse> message = apiClient().getMessage(testAccountName, queueName, 3L).execute();

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
                clock.tickSeconds(4L);
                continue;
            }
            else {
                assertThat(popReceipt).isNotNull();

                final Response<ResponseBody> ackResponse = apiClient().ackMessage(testAccountName, queueName, popReceipt).execute();

                System.out.println("ACK");

                assertThat(ackResponse.isSuccess()).isTrue();
            }

            clock.tick();
        }
    }

    @Test
    public void delete_queue() throws IOException {
        final QueueName delete_queue = QueueName.valueOf("delete_queue");

        apiClient().createQueue(testAccountName, new QueueCreateOptions(delete_queue)).execute();

        assertThat(apiClient().deleteQueue(testAccountName, delete_queue).execute().isSuccess()).isTrue();

        assertThat(apiClient().getMessage(testAccountName, delete_queue).execute().isSuccess()).isFalse();
    }

    @Test
    public void delete_on_non_existent_queue_fails() throws Exception {
        final QueueName non_existent_queue = QueueName.valueOf("non_existent_queue");

        assertThat(apiClient().deleteQueue(testAccountName, non_existent_queue).execute().isSuccess()).isFalse();
    }

    @Test
    public void update_message() throws Exception {
        final QueueName delete_queue = QueueName.valueOf("update_message");

        apiClient().createQueue(testAccountName, new QueueCreateOptions(delete_queue)).execute();

        apiClient().addMessage(testAccountName, delete_queue, "foo").execute();

        final GetMessageResponse body = apiClient().getMessage(testAccountName, delete_queue).execute().body();

        final UpdateMessageResponse updateResponse =
                apiClient().updateMessage(
                        testAccountName,
                        delete_queue,
                        body.getPopReceipt(),
                        new UpdateMessageRequest("foo2", 10L)).execute()
                           .body();

        apiClient().ackMessage(testAccountName, delete_queue, updateResponse.getPopReceipt()).execute();

        assertThat(apiClient().getMessage(testAccountName, delete_queue).execute().code()).isEqualTo(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test(timeout = 30000)
    public void test_invis_like_crazy() throws Exception {
        final QueueName queueName = QueueName.valueOf("test");

        apiClient().createQueue(testAccountName, new QueueCreateOptions(queueName)).execute();

        int count = 100;

        for (int i = 0; i < count; i++) {
            apiClient().addMessage(testAccountName, queueName, Integer.valueOf(i).toString()).execute();
        }

        final TestClock clock = getTestClock();

        GetMessageResponse body;
        int i = 0;

        while ((body = getMessageWithThreeSecondInvis(apiClient(), queueName)) != null && count > 0) {

            final String popReceipt = body.getPopReceipt();

            System.out.println(String.format("Message id: %s, Delivery count %s", body.getMessage(), body.getDeliveryCount()));

            if (i++ % 20 == 0 && body.getDeliveryCount() < 4) {
                // message times out
                clock.tickSeconds(5L);
            }
            else {
                assertThat(popReceipt).isNotNull();

                final Response<ResponseBody> ackResponse = apiClient().ackMessage(testAccountName, queueName, popReceipt).execute();

                assertThat(ackResponse.isSuccess()).isTrue();

                count--;
            }
        }
    }

    private GetMessageResponse getMessageWithThreeSecondInvis(final CassieqApi client, final QueueName queueName) throws Exception {
        final Response<GetMessageResponse> message =
                client.getMessage(testAccountName, queueName, 3L).execute();

        return message.body();
    }

}
