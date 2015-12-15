package io.paradoxical.cassieq.unittests;

import categories.StressTests;
import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.api.client.CassandraQueueApi;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.modules.TestClockModule;
import io.paradoxical.cassieq.unittests.server.SelfHostServer;
import io.paradoxical.cassieq.unittests.time.TestClock;
import io.paradoxical.common.test.junit.RetryRule;
import lombok.Cleanup;
import org.apache.commons.collections4.ListUtils;
import org.jooq.lambda.Unchecked;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Category(StressTests.class)
public class SlowTests extends TestBase {
    private static final Logger logger = getLogger(SlowTests.class);

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Test
    public void test_multiple_parallel_readers() throws Exception {
        parallel_read_worker(250, // messages
                             10,  // good workers
                             1,   // bad workers
                             Duration.ofSeconds(30));
    }

    private void parallel_read_worker(int numMessages, int numGoodWorkers, int numBadWorkers, Duration testTimeout) throws InterruptedException, IOException {

        final TestClock testClock = new TestClock();

        @Cleanup("stop") SelfHostServer server = new SelfHostServer(
                new InMemorySessionProvider(session),
                new TestClockModule(testClock));

        server.start();

        final Collection<Integer> payloads = Collections.synchronizedCollection(new ArrayList<>());

        final CassandraQueueApi client = CassandraQueueApi.createClient(server.getBaseUri());

        final QueueName queueName = QueueName.valueOf(String.valueOf(new Random().nextInt()));

        client.createQueue(new QueueCreateOptions(queueName)).execute();

        new Thread(() -> {
            IntStream.range(0, numMessages)
                     .forEach(Unchecked.intConsumer(i -> {
                         client.addMessage(queueName, i).execute();
                     }));
        }).start();

        final ExecutorService executorService = Executors.newFixedThreadPool(40);

        final List<Worker> goodWorkers = IntStream.range(0, numGoodWorkers)
                                                  .mapToObj(i -> new Worker(client, queueName, payloads, executorService, testClock))
                                                  .collect(toList());

        final List<Worker> badWorkers = IntStream.range(0, numBadWorkers)
                                                 .mapToObj(i -> new FailingWorker(client, queueName, payloads, executorService))
                                                 .collect(toList());

        final List<Worker> workers = ListUtils.union(badWorkers, goodWorkers);

        workers.forEach(executorService::submit);

        final LocalTime start = LocalTime.now();

        final LocalTime end = start.plus(testTimeout);

        while (payloads.size() != numMessages && LocalTime.now().isBefore(end)) {
            Thread.sleep(50);
        }

        workers.forEach(Worker::stop);

        assertThat(payloads.size()).isEqualTo(numMessages);
    }

    class BadWorkerException extends RuntimeException {}

    class FailingWorker extends Worker {
        public FailingWorker(
                final CassandraQueueApi client,
                final QueueName queueName,
                final Collection<Integer> collection,
                final ExecutorService executorService) {
            super(client, queueName, collection, executorService, new TestClock());
        }

        @Override
        protected Integer getMessage() throws IOException {
            throw new BadWorkerException();
        }
    }

    class Worker implements Runnable {
        private final CassandraQueueApi client;
        private final QueueName queueName;
        private final Collection<Integer> collection;
        private final ExecutorService executorService;
        private final TestClock testClock;
        private volatile boolean running = true;


        public Worker(
                CassandraQueueApi client,
                QueueName queueName,
                Collection<Integer> results,
                final ExecutorService executorService, final TestClock testClock) {
            this.client = client;
            this.queueName = queueName;
            this.collection = results;
            this.executorService = executorService;
            this.testClock = testClock;
        }

        @Override
        public void run() {
            try {
                if (!running) {
                    return;
                }

                Integer i = getMessage();

                if (i != null) {
                    logger.success("Message: " + i);

                    collection.add(i);
                }
                else if (testClock != null) {
                    logger.info("TICK!");

                    testClock.tickSeconds(5L);
                }
            }
            catch (BadWorkerException ex) {
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

                executorService.submit(this);
            }
        }

        public void stop() {
            running = false;
        }

        protected Integer getMessage() throws IOException {
            final GetMessageResponse body = client.getMessage(queueName, 2L).execute().body();

            if (body != null) {
                if (client.ackMessage(queueName, body.getPopReceipt()).execute().isSuccess()) {
                    return Integer.parseInt(body.getMessage());
                }
            }

            return null;
        }
    }
}
