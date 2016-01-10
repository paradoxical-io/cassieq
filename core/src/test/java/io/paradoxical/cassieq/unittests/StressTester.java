package io.paradoxical.cassieq.unittests;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.api.client.CassandraQueueApi;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueueName;
import org.junit.Ignore;
import org.junit.Test;
import retrofit.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.Random;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class StressTester {
    private static final Logger logger = getLogger(StressTester.class);

    @Ignore
    @Test
    public void stress_test() throws InterruptedException {
        final CassandraQueueApi client = CassandraQueueApi.createClient("http://localhost:8080");

        final QueueName perftest = QueueName.valueOf("perftest1");

        for (int i = 0; i < 50; i++) {
            startWriter(client, perftest);
        }

//        for (int i = 0; i < 5; i++) {
//            startReader(client, perftest);
//        }

        Thread.sleep(Duration.ofDays(1).toMillis());
    }

    private void startReader(final CassandraQueueApi client, final QueueName perftest) {
        new Thread(() -> {
            while (true) {
                try {
                    final Response<GetMessageResponse> execute = client.getMessage(perftest, 30L).execute();

                    final String popReceipt = execute.body().getPopReceipt();

                    client.ackMessage(perftest, popReceipt).execute();
                }
                catch (IOException e) {
                    logger.error(e, "Error");
                }
            }
        }).start();
    }

    private void startWriter(final CassandraQueueApi client, final QueueName perftest) {
        new Thread(() -> {
            final Random random = new Random();
            while (true) {
                try {
                    client.addMessage(perftest, random.nextInt()).execute();
                }
                catch (IOException e) {
                    logger.error(e, "Error");
                }
            }
        }).start();
    }
}
