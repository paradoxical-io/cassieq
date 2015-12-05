package io.paradoxical.cassieq.unittests;

import io.paradoxical.cassieq.api.client.CassandraQueueApi;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.server.SelfHostServer;
import lombok.Cleanup;
import org.jooq.lambda.Unchecked;
import org.junit.Ignore;
import org.junit.experimental.categories.Category;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Category(SlowTests.class)
public class SlowTests extends TestBase {
    @Test
    @Ignore
    public void test_multiple_parallel_readers() throws Exception {
        @Cleanup SelfHostServer server = new SelfHostServer(new InMemorySessionProvider(session));

        server.start();

        final Collection<Integer> payloads = Collections.synchronizedCollection(new ArrayList<>());

        final CassandraQueueApi client = CassandraQueueApi.createClient(server.getBaseUri());

        final QueueName queueName = QueueName.valueOf("test_multiple_parallel_readers");

        client.createQueue(new QueueCreateOptions(queueName)).execute();

        final int messagePublish = 20;

        new Thread(() -> {
            IntStream.range(0, messagePublish)
                     .parallel()
                     .forEach(Unchecked.intConsumer(i -> {
                         client.addMessage(queueName, Integer.valueOf(i).toString()).execute();
                     }));
        }).start();

        final ExecutorService executorService = Executors.newFixedThreadPool(40);

        for (int i = 0; i < 40; i++) {
            executorService.submit(new Worker(client, queueName, payloads));
        }

        Thread.sleep(1000);
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

                        return;
                    }

                    Thread.sleep(50);
                }
                catch (Exception ex) {
                }
            }
        }

        private Integer getMessage() throws IOException {
            final GetMessageResponse body = client.getMessage(queueName).execute().body();

            if (body != null) {
                if (client.ackMessage(queueName, body.getPopReceipt()).execute().isSuccess()) {
                    return Integer.valueOf(body.getMessage());
                }
            }

            return null;
        }
    }
}
