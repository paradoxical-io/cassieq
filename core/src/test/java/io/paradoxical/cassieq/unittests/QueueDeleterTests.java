package io.paradoxical.cassieq.unittests;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.DeletionJob;
import io.paradoxical.cassieq.dataAccess.MessageDeletorJobProcessorImpl;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueAlreadyDeletingException;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageDeleterJobProcessor;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.MessageDeleterJobProcessorFactory;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.unittests.modules.MessageDeletorJobModule;
import io.paradoxical.cassieq.workers.QueueDeleter;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class QueueDeleterTests extends TestBase {
    @Test
    public void can_create_queue_while_job_is_deleting() throws QueueAlreadyDeletingException, InterruptedException {

        final MessageDeleterJobProcessorFactory jobSpy = spy(MessageDeleterJobProcessorFactory.class);

        final Injector defaultInjector = getDefaultInjector(new MessageDeletorJobModule(jobSpy));

        final Semaphore start = new Semaphore(1);

        final Thread[] deletion = new Thread[1];

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

                    realDeletor.start();
                }
                catch (Exception e) {
                    System.out.println(e);
                }
            });

            deletion[0].start();

            return mock(MessageDeleterJobProcessor.class);
        });


        final QueueDeleter queueDeleter = defaultInjector.getInstance(QueueDeleter.class);

        final QueueRepository instance = defaultInjector.getInstance(QueueRepository.class);

        final QueueName name = QueueName.valueOf("can_create_queue_while_job_is_deleting");

        final QueueDefinition build = QueueDefinition.builder().queueName(name).build();

        instance.createQueue(build);

        queueDeleter.delete(name);

        // should be able to make new queue
        final Optional<QueueDefinition> queue = instance.createQueue(build);

        assertThat(queue).isPresent();
        assertThat(queue.get().getVersion()).isEqualTo(1);

        start.release();

        deletion[0].join();

        final QueueDefinition activeQueue = instance.getActiveQueue(queue.get().getQueueName()).get();

        // make sure the deletor when it completed didn't just kill this active queue
        assertThat(activeQueue.getVersion()).isEqualTo(queue.get().getVersion());
    }

    @Test
    public void test_deleter_cleans_up_pointers() throws QueueAlreadyDeletingException {
        final QueueDeleter deleter = getDefaultInjector().getInstance(QueueDeleter.class);

        final QueueRepository instance = getDefaultInjector().getInstance(QueueRepository.class);

        final QueueName name = QueueName.valueOf("test_deleter_cleans_up_pointers");

        final QueueDefinition build = QueueDefinition.builder().queueName(name).build();

        instance.createQueue(build);

        final DataContextFactory contextFactory = getDefaultInjector().getInstance(DataContextFactory.class);

        final DataContext dataContext = contextFactory.forQueue(build);

        // move monton up
        dataContext.getMonotonicRepository().nextMonotonic();
        dataContext.getPointerRepository().tryMoveInvisiblityPointerTo(InvisibilityMessagePointer.valueOf(0), InvisibilityMessagePointer.valueOf(10));
        dataContext.getPointerRepository().advanceMessageBucketPointer(ReaderBucketPointer.valueOf(0), ReaderBucketPointer.valueOf(10));

        deleter.delete(name);

        final MonotonicIndex current = dataContext.getMonotonicRepository().getCurrent();

        // we return 0 if we dont know or it doesnt exist

        assertThat(current.get()).isEqualTo(0);

        final InvisibilityMessagePointer currentInvisPointer = dataContext.getPointerRepository().getCurrentInvisPointer();

        assertThat(currentInvisPointer.get()).isEqualTo(0);

        final ReaderBucketPointer readerCurrentBucket = dataContext.getPointerRepository().getReaderCurrentBucket();

        assertThat(readerCurrentBucket.get()).isEqualTo(0);
    }
}
