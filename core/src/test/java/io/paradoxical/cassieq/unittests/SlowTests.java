package io.paradoxical.cassieq.unittests;

import categories.StressTests;
import categories.VerySlowTests;
import com.godaddy.logging.Logger;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.inject.Injector;
import com.squareup.okhttp.ResponseBody;
import io.paradoxical.cassieq.api.client.CassandraQueueApi;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.QueueDataContext;
import io.paradoxical.cassieq.factories.QueueRepositoryFactory;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.unittests.modules.HazelcastTestModule;
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
import retrofit.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

@Category({ StressTests.class, VerySlowTests.class })
public class SlowTests extends TestBase {
    private static final Logger logger = getLogger(SlowTests.class);

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Test(timeout = 30000)
    public void test_multiple_parallel_readers() throws Exception {
        parallel_read_worker(250, // messages
                             10,  // good workers
                             1);  // bad workers
    }

    private void parallel_read_worker(int numMessages, int numGoodWorkers, int numBadWorkers) throws InterruptedException, IOException {

        final TestClock testClock = new TestClock();

        @Cleanup("stop") SelfHostServer server = new SelfHostServer(
                new InMemorySessionProvider(session),
                new HazelcastTestModule(),
                new TestClockModule(testClock));

        server.start();

        final Collection<Integer> counter = ConcurrentHashMultiset.create();

        final CassandraQueueApi client = CassandraQueueApi.createClient(server.getBaseUri());

        final QueueName queueName = QueueName.valueOf(String.valueOf(new Random().nextInt()));


        final QueueCreateOptions queueCreateOptions =
                QueueCreateOptions.builder()
                                  .queueName(queueName)
                                  .deleteBucketsAfterFinalize(false)
                                  .repairWorkerPollSeconds(1)
                                  .build();

        client.createQueue(testAccountName, queueCreateOptions)
              .execute();

        final Thread thread = new Thread(() -> {
            IntStream.range(0, numMessages)
                     .parallel()
                     .forEach(Unchecked.intConsumer(i -> {
                         client.addMessage(testAccountName, queueName, i).execute();
                     }));
        });

        thread.start();

        final ExecutorService executorService = Executors.newFixedThreadPool(40);

        final List<Worker> goodWorkers = IntStream.range(0, numGoodWorkers)
                                                  .mapToObj(i -> new Worker(client, queueName, counter, executorService, testClock))
                                                  .collect(toList());

        final List<Worker> badWorkers = IntStream.range(0, numBadWorkers)
                                                 .mapToObj(i -> new FailingWorker(client, queueName, counter, executorService))
                                                 .collect(toList());

        final List<Worker> workers = ListUtils.union(badWorkers, goodWorkers);

        workers.forEach(executorService::submit);

        while (counter.stream().distinct().count() != numMessages) {
            Thread.sleep(50);
        }

        thread.join();

        workers.forEach(Worker::stop);

        assertThat(counter.stream().distinct().count()).isEqualTo(numMessages);

        assertAllMessagesDeliveredAtLeastOnce(server, numMessages, queueCreateOptions);
    }

    private void assertAllMessagesDeliveredAtLeastOnce(
            final SelfHostServer server,
            final int numMessages,
            final QueueCreateOptions queueCreateOptions) {
        final Injector injector = server.getService().getGuiceBundleProvider().getBundle().getInjector();

        final DataContextFactory instance = injector.getInstance(DataContextFactory.class);
        final QueueRepositoryFactory queueRepositoryFactory = injector.getInstance(QueueRepositoryFactory.class);
        final AccountRepository accountRepository = injector.getInstance(AccountRepository.class);

        final AccountName test = AccountName.valueOf("test");
        accountRepository.createAccount(test);

        final QueueRepository queueRepository = queueRepositoryFactory.forAccount(test);

        final Optional<QueueDefinition> definition = queueRepository.getActiveQueue(queueCreateOptions.getQueueName());

        final QueueDataContext dataContext = instance.forQueue(definition.get());

        int messagesFound = 0;

        for (int i = 0; ; i++) {
            final List<Message> messages = dataContext.getMessageRepository().getMessages(ReaderBucketPointer.valueOf(i));

            assertThat(messages.stream().allMatch(Message::isAcked)).isTrue();

            messagesFound += messages.stream().filter(m -> m.isAcked() && m.getDeliveryCount() > 0).count();

            if (messages.size() == 0) {
                break;
            }
        }

        assertThat(messagesFound).isEqualTo(numMessages);

        final Long queueSize = queueRepository.getQueueSize(definition.get()).get();

        assertThat(queueSize).isEqualTo(0);
    }

    class BadWorkerException extends RuntimeException {}

    class FailingWorker extends Worker {
        public FailingWorker(
                final CassandraQueueApi client,
                final QueueName queueName,
                final Collection<Integer> counter,
                final ExecutorService executorService) {
            super(client, queueName, counter, executorService, new TestClock());
        }

        @Override
        protected Integer getMessage() throws IOException {
            client.getMessage(testAccountName, queueName, 10L).execute().body();

            throw new BadWorkerException();
        }
    }

    class Worker implements Runnable {
        protected final CassandraQueueApi client;
        protected final QueueName queueName;
        private final Collection<Integer> counter;
        private final ExecutorService executorService;
        private final TestClock testClock;
        private volatile boolean running = true;


        public Worker(
                CassandraQueueApi client,
                QueueName queueName,
                Collection<Integer> counter,
                final ExecutorService executorService, final TestClock testClock) {
            this.client = client;
            this.queueName = queueName;
            this.counter = counter;
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

                    counter.add(i);
                }
                else if (testClock != null) {
                    logger.info("TICK!");

                    testClock.tickSeconds(1L);
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
            final GetMessageResponse body = client.getMessage(testAccountName, queueName, 10L).execute().body();

            if (body != null) {
                final Response<ResponseBody> execute = client.ackMessage(testAccountName, queueName, body.getPopReceipt()).execute();
                if (execute.isSuccess()) {
                    logger.info("ACKED!");

                    return Integer.parseInt(body.getMessage());
                }
                else {
                    testClock.tickSeconds(1L);

                    fail("Failure to ack!");
                }
            }

            return null;
        }
    }
}
