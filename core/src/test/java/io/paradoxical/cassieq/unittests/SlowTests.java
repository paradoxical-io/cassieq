package io.paradoxical.cassieq.unittests;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.api.client.CassandraQueueApi;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.server.SelfHostServer;
import lombok.Cleanup;
import org.jooq.lambda.Unchecked;
import org.junit.experimental.categories.Category;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Category(SlowTests.class)
public class SlowTests extends TestBase {
    private static final Logger logger = getLogger(SlowTests.class);

    @Test
    public void test_multiple_parallel_readers() throws Exception {
        parallel_read_worker(250, // messages
                             20,  // good workers
                             5,   // bad workers
                             Duration.ofSeconds(60));
    }

    private void parallel_read_worker(int numMessages, int numGoodWorkers, int numBadWorkers, Duration testTimeout) throws InterruptedException, IOException {
        @Cleanup("stop") SelfHostServer server = new SelfHostServer(new InMemorySessionProvider(session));

        server.start();

        final Collection<Integer> payloads = Collections.synchronizedCollection(new ArrayList<>());

        final CassandraQueueApi client = CassandraQueueApi.createClient(server.getBaseUri());

        final QueueName queueName = QueueName.valueOf(String.valueOf(new Random().nextInt()));

        client.createQueue(new QueueCreateOptions(queueName)).execute();

        new Thread(() -> {
            IntStream.range(0, numMessages)
                     .parallel()
                     .forEach(Unchecked.intConsumer(i -> {
                         client.addMessage(queueName, i).execute();
                     }));
        }).start();

        final ExecutorService executorService = Executors.newFixedThreadPool(40);

        // N good workesr to pull stuff off
        for (int i = 0; i < numGoodWorkers; i++) {
            executorService.submit(new Worker(client, queueName, payloads));
        }

        // N/5 bad workers who pulls things off but can't ever finish
        for (int i = 0; i < numBadWorkers; i++) {
            executorService.submit(new FailingWorker(client, queueName, payloads));
        }

        final LocalTime start = LocalTime.now();

        final LocalTime end = start.plus(testTimeout);

        while (payloads.size() != numMessages && LocalTime.now().isBefore(end)) {
            Thread.sleep(50);
        }

        assertThat(payloads.size()).isEqualTo(numMessages);
    }

    class FailingWorker extends Worker {
        public FailingWorker(final CassandraQueueApi client, final QueueName queueName, final Collection<Integer> collection) {
            super(client, queueName, collection);
        }

        @Override protected Integer getMessage() throws IOException {
            throw new RuntimeException("I am a bad worker!");
        }
    }

    class Worker implements Runnable {
        private final CassandraQueueApi client;
        private final QueueName queueName;
        private final Collection<Integer> collection;

        public Worker(CassandraQueueApi client, QueueName queueName, Collection<Integer> collection) {
            this.client = client;
            this.queueName = queueName;
            this.collection = collection;
        }

        @Override public void run() {
            while (true) {
                try {
                    Integer i = getMessage();

                    if (i != null) {
                        collection.add(i);
                    }
                }
                catch (Exception ex) {
                    logger.error(ex, "Error");
                }
                finally {
                    try {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e) {
                        logger.error(e, "Error");
                    }
                }
            }
        }

        protected Integer getMessage() throws IOException {
            final GetMessageResponse body = client.getMessage(queueName, 5L).execute().body();

            if (body != null) {
                if (client.ackMessage(queueName, body.getPopReceipt()).execute().isSuccess()) {
                    return Integer.parseInt(body.getMessage());
                }
            }

            return null;
        }
    }
}
