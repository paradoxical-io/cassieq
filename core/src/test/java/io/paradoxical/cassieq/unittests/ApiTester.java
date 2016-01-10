package io.paradoxical.cassieq.unittests;

import com.godaddy.logging.Logger;
import com.squareup.okhttp.ResponseBody;
import io.paradoxical.cassieq.api.client.CassandraQueueApi;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.UpdateMessageRequest;
import io.paradoxical.cassieq.model.UpdateMessageResponse;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.server.SelfHostServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import retrofit.Response;

import java.io.IOException;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiTester extends TestBase {

    private static final Logger logger = getLogger(ApiTester.class);

    private static SelfHostServer server;

    private static CassandraQueueApi client;

    @BeforeClass
    public static void setup() {
        server = new SelfHostServer(new InMemorySessionProvider(session));

        server.start();

        client = CassandraQueueApi.createClient(server.getBaseUri().toString());
    }

    @AfterClass
    public static void cleanup() {
        server.stop();
    }

    @Test
    public void put_into_deleted_queue_fails() throws IOException {
        final QueueName queueName = QueueName.valueOf("put_into_deleted_queue_fails");

        client.createQueue(new QueueCreateOptions(queueName)).execute();

        client.deleteQueue(queueName).execute();

        final Response<ResponseBody> result = client.addMessage(queueName, "foo").execute();

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void test_client_can_create_put_and_ack() throws Exception {
        final QueueName queueName = QueueName.valueOf("test_client_can_create_put_and_ack");

        client.createQueue(new QueueCreateOptions(queueName)).execute();

        client.addMessage(queueName, "hi").execute();

        getTestClock().tick();

        final Response<GetMessageResponse> message = client.getMessage(queueName).execute();

        final GetMessageResponse body = message.body();

        assertThat(body).isNotNull();

        final String popReceipt = body.getPopReceipt();

        assertThat(popReceipt).isNotNull();

        final Response<ResponseBody> ackResponse = client.ackMessage(queueName, popReceipt).execute();

        assertThat(ackResponse.isSuccess()).isTrue();
    }

    @Test
    public void demo_invis_client() throws Exception {
        final QueueName queueName = QueueName.valueOf("demo_invis_client");

        client.createQueue(new QueueCreateOptions(queueName)).execute();

        int count = 21;

        for (int i = 0; i < count; i++) {
            client.addMessage(queueName, Integer.valueOf(i).toString()).execute();
        }

        int c = -1;

        while (true) {
            c++;
            final Response<GetMessageResponse> message = client.getMessage(queueName, 1L).execute();

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

                final Response<ResponseBody> ackResponse = client.ackMessage(queueName, popReceipt).execute();

                System.out.println("ACK");

                assertThat(ackResponse.isSuccess()).isTrue();
            }
        }
    }

    @Test
    public void delete_queue() throws IOException {
        final QueueName delete_queue = QueueName.valueOf("delete_queue");

        client.createQueue(new QueueCreateOptions(delete_queue)).execute();

        assertThat(client.deleteQueue(delete_queue).execute().isSuccess()).isTrue();

        assertThat(client.getMessage(delete_queue).execute().isSuccess()).isFalse();
    }

    @Test
    public void update_message() throws Exception {
        final QueueName delete_queue = QueueName.valueOf("update_message");

        client.createQueue(new QueueCreateOptions(delete_queue)).execute();


        client.addMessage(delete_queue, "foo").execute();

        final GetMessageResponse body = client.getMessage(delete_queue).execute().body();

        final UpdateMessageResponse updateResponse = client.updateMessage(delete_queue, body.getPopReceipt(),
                                                                          new UpdateMessageRequest("foo2", 10L)).execute()
                                                           .body();

        client.ackMessage(delete_queue, updateResponse.getPopReceipt()).execute();

        assertThat(client.getMessage(delete_queue).execute().code()).isEqualTo(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test(timeout = 30000)
    public void invis() throws Exception {
        final QueueName queueName = QueueName.valueOf("test");

        client.createQueue(new QueueCreateOptions(queueName)).execute();

        int count = 100;

        for (int i = 0; i < count; i++) {
            client.addMessage(queueName, Integer.valueOf(i).toString()).execute();
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

                final Response<ResponseBody> ackResponse = client.ackMessage(queueName, popReceipt).execute();

                assertThat(ackResponse.isSuccess()).isTrue();

                count--;
            }

        }
    }

    private GetMessageResponse getMessage(final CassandraQueueApi client, final QueueName queueName) throws java.io.IOException {
        final Response<GetMessageResponse> message = client.getMessage(queueName, 3L).execute();

        return message.body();
    }

}
