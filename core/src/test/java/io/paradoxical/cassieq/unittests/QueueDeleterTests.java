package io.paradoxical.cassieq.unittests;

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
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.MessageDeleterJobProcessorFactory;
import io.paradoxical.cassieq.factories.QueueDataContext;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.modules.MessageDeletorJobModule;
import io.paradoxical.cassieq.workers.QueueDeleter;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.Semaphore;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class QueueDeleterTests extends TestBase {
    private static final Logger logger = getLogger(QueueDeleterTests.class);

    @Test
    public void can_create_queue_while_job_is_deleting() throws Exception {

        final MessageDeleterJobProcessorFactory jobSpy = spy(MessageDeleterJobProcessorFactory.class);

        final Injector defaultInjector = getDefaultInjector(new MessageDeletorJobModule(jobSpy),
                                                            new InMemorySessionProvider(session));

        final Semaphore start = new Semaphore(1);

        final Thread[] deletion = new Thread[1];

        start.acquire();

        // delay the actual queue deletion as if it was an async job
        when(jobSpy.createDeletionProcessor(any())).thenAnswer(answer -> {
            deletion[0] = new Thread(() -> {
                try {
                    start.acquire();

                    final MessageDeletorJobProcessorImpl realDeletor = defaultInjector.createChildInjector(new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(DeletionJob.class)
                                    .annotatedWith(Assisted.class)
                                    .toInstance(((DeletionJob) answer.getArguments()[0]));
                        }
                    }).getInstance(MessageDeletorJobProcessorImpl.class);

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

            deletion[0].start();

            return mock(MessageDeleterJobProcessor.class);
        });


        final QueueDeleter queueDeleter = defaultInjector.getInstance(QueueDeleter.class);

        final QueueRepository instance = defaultInjector.getInstance(QueueRepository.class);

        final QueueName name = QueueName.valueOf("can_create_queue_while_job_is_deleting");

        final QueueDefinition build = QueueDefinition.builder().queueName(name).build();

        final Optional<QueueDefinition> initialQueue = instance.createQueue(build);

        // make sure we got a v0 queue
        assertThat(initialQueue.get().getVersion()).isEqualTo(0);

        // delete v0 async
        queueDeleter.delete(name);

        // should be able to make new queue
        final Optional<QueueDefinition> queue = instance.createQueue(build);

        // make sure its a v1
        assertThat(queue).isPresent();
        assertThat(queue.get().getVersion()).isEqualTo(1);

        // let the deleter process v0
        start.release();

        // wait for deleter to finish
        deletion[0].join();

        final QueueDefinition activeQueue = instance.getActiveQueue(queue.get().getQueueName()).get();

        // make sure the deleter when it completed didn't just kill this active queue (v1)
        assertThat(activeQueue.getVersion()).isEqualTo(queue.get().getVersion());
    }

    @Test
    public void test_deleter_cleans_up_pointers() throws QueueAlreadyDeletingException, ExistingMonotonFoundException {
        final QueueDeleter deleter = getDefaultInjector().getInstance(QueueDeleter.class);

        final QueueRepository queueRepository = getDefaultInjector().getInstance(QueueRepository.class);

        final QueueName name = QueueName.valueOf("test_deleter_cleans_up_pointers");

        final QueueDefinition definition = QueueDefinition.builder().queueName(name).build();

        queueRepository.createQueue(definition);

        final DataContextFactory contextFactory = getDefaultInjector().getInstance(DataContextFactory.class);

        final QueueDataContext dataContext = contextFactory.forQueue(definition);

        // move monton up
        dataContext.getMessageRepository()
                   .putMessage(Message.builder().blob("test")
                                      .index(dataContext.getMonotonicRepository().nextMonotonic())
                                      .build());

        dataContext.getMonotonicRepository().nextMonotonic();
        dataContext.getPointerRepository().tryMoveInvisiblityPointerTo(InvisibilityMessagePointer.valueOf(0), InvisibilityMessagePointer.valueOf(10));
        dataContext.getPointerRepository().advanceMessageBucketPointer(ReaderBucketPointer.valueOf(0), ReaderBucketPointer.valueOf(10));

        assertThat(queueRepository.getQueueSize(definition).get()).isEqualTo(1);

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
