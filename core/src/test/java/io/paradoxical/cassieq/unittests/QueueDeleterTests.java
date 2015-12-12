package io.paradoxical.cassieq.unittests;

import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.workers.QueueDeleter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueueDeleterTests extends TestBase {
    @Test
    public void test_deleter_cleans_up_pointers() {
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
