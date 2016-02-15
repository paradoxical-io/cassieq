package io.paradoxical.cassieq.unittests.tests.queueSemantics;

import categories.BuildVerification;
import com.godaddy.logging.Logger;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.DeletionJob;
import io.paradoxical.cassieq.dataAccess.MessageDeletorJobProcessorImpl;
import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueAlreadyDeletingException;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageDeleterJobProcessor;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.MessageDeleterJobProcessorFactory;
import io.paradoxical.cassieq.factories.QueueDataContext;
import io.paradoxical.cassieq.factories.QueueRepositoryFactory;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.unittests.DbTestBase;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.modules.MessageDeletorJobModule;
import io.paradoxical.cassieq.workers.QueueDeleter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Optional;
import java.util.concurrent.Semaphore;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Category(BuildVerification.class)
public class QueueDeleterTests extends DbTestBase {
    private static final Logger logger = getLogger(QueueDeleterTests.class);

    @Test
    public void can_create_queue_while_job_is_deleting() throws Exception {

        final MessageDeleterJobProcessorFactory jobSpy = spy(MessageDeleterJobProcessorFactory.class);

        final Injector defaultInjector = getDefaultInjector(new MessageDeletorJobModule(jobSpy),
                                                            new InMemorySessionProvider(session));

        final Semaphore start = new Semaphore(1);

        final Thread[] deletionThreadBox = new Thread[1];

        start.acquire();

        // delay the actual queue deletionThreadBox as if it was an async job
        when(jobSpy.createDeletionProcessor(any())).thenAnswer(answer -> {
            final DeletionJob deletionJob = (DeletionJob) answer.getArguments()[0];

            deletionThreadBox[0] = new Thread(() -> {
                try {
                    start.acquire();

                    final Injector childInjector = defaultInjector.createChildInjector(new AbstractModule() {
                        @Override
                        protected void configure() {

                            bind(DeletionJob.class)
                                    .annotatedWith(Assisted.class)
                                    .toInstance(deletionJob);
                        }
                    });

                    final MessageDeletorJobProcessorImpl realDeletor = childInjector.getInstance(MessageDeletorJobProcessorImpl.class);

                    // the job starts, and after completion tries to mark the queue as inactive
                    // but we have already created a new active queue so this should NOT occur

                    logger.info("Starting deleter");

                    realDeletor.start();

                    logger.info("Done deleter");
                }
                catch (Exception ex) {
                    logger.error(ex, "Error creating deleter");
                }
            });

            deletionThreadBox[0].start();

            return mock(MessageDeleterJobProcessor.class);
        });

        final QueueDeleter.Factory deleterFactory = defaultInjector.getInstance(QueueDeleter.Factory.class);
        final QueueDeleter queueDeleter = deleterFactory.create(testAccountName);
        final QueueRepositoryFactory queueRepositoryFactory = defaultInjector.getInstance(QueueRepositoryFactory.class);
        final QueueRepository queueRepository = queueRepositoryFactory.forAccount(testAccountName);

        final QueueName name = QueueName.valueOf("can_create_queue_while_job_is_deleting");

        final QueueDefinition build = QueueDefinition.builder().accountName(testAccountName).queueName(name).build();

        final Optional<QueueDefinition> initialQueue = queueRepository.createQueue(build);

        // make sure we got a v0 queue
        assertThat(initialQueue
                           .orElseThrow(() -> new RuntimeException("Couldn't create queue or it already existed.."))
                           .getVersion()).isEqualTo(0)
                                         .withFailMessage("Couldn't get correct queue version");

        // delete v0 async
        queueDeleter.delete(name);

        assertThat(deletionThreadBox[0]).isNotNull();

        // should be able to make new queue
        final Optional<QueueDefinition> queue = queueRepository.createQueue(build);

        // make sure its a v1
        assertThat(queue).isPresent();
        assertThat(queue.get().getVersion()).isEqualTo(1);

        // let the deleter process v0
        start.release();

        // wait for deleter to finish
        deletionThreadBox[0].join();

        final QueueDefinition activeQueue = queueRepository.getActiveQueue(queue.get().getQueueName()).get();

        // make sure the deleter when it completed didn't just kill this active queue (v1)
        assertThat(activeQueue.getVersion()).isEqualTo(queue.get().getVersion());
    }

    @Test
    public void test_deleter_cleans_up_pointers() throws QueueAlreadyDeletingException, ExistingMonotonFoundException {
        final QueueDeleter.Factory deleterFactory = getDefaultInjector().getInstance(QueueDeleter.Factory.class);

        final QueueDeleter deleter = deleterFactory.create(testAccountName);

        final DataContextFactory contextFactory = getDefaultInjector().getInstance(DataContextFactory.class);

        final QueueName name = QueueName.valueOf("test_deleter_cleans_up_pointers");
        final QueueRepository queueRepository = contextFactory.forAccount(testAccountName);


        final QueueDefinition definition = queueRepository.createQueue(QueueDefinition.builder()
                                                                                      .accountName(testAccountName)
                                                                                      .queueName(name).build()).get();

        final QueueDataContext dataContext = contextFactory.forQueue(definition);

        // move monton up
        dataContext.getMessageRepository()
                   .putMessage(Message.builder().blob("test")
                                      .index(dataContext.getMonotonicRepository().nextMonotonic())
                                      .build());

        dataContext.getMonotonicRepository().nextMonotonic();
        dataContext.getPointerRepository().tryMoveInvisiblityPointerTo(InvisibilityMessagePointer.valueOf(0), InvisibilityMessagePointer.valueOf(10));
        dataContext.getPointerRepository().advanceMessageBucketPointer(ReaderBucketPointer.valueOf(0), ReaderBucketPointer.valueOf(10));

        assertThat(queueRepository.getQueueSize(definition)
                                  .orElse(0L))
                .isEqualTo(1)
                .withFailMessage("Queue Size wasn't updated");

        deleter.delete(name);

        final MonotonicIndex current = dataContext.getMonotonicRepository().getCurrent();

        // we return 0 if we dont know or it doesnt exist

        assertThat(current.get()).isEqualTo(0);

        final InvisibilityMessagePointer currentInvisPointer = dataContext.getPointerRepository().getCurrentInvisPointer();

        assertThat(currentInvisPointer.get()).isEqualTo(0);

        final ReaderBucketPointer readerCurrentBucket = dataContext.getPointerRepository().getReaderCurrentBucket();

        assertThat(readerCurrentBucket.get()).isEqualTo(0);

        assertThat(queueRepository.getQueueSize(definition)).isEmpty();
    }
}
