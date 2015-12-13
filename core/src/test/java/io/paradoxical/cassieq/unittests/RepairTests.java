package io.paradoxical.cassieq.unittests;

import com.google.inject.Injector;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MessageTag;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import io.paradoxical.cassieq.workers.BucketConfiguration;
import io.paradoxical.cassieq.workers.repair.RepairWorkerImpl;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;
import io.paradoxical.cassieq.workers.repair.SimpleRepairWorkerManager;
import org.joda.time.Duration;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class RepairTests extends TestBase {
    @Test
    public void repairer_republishes_newly_visible_in_tombstoned_bucket() throws InterruptedException, ExistingMonotonFoundException, ExecutionException {

        final ServiceConfiguration serviceConfiguration = new ServiceConfiguration();

        final BucketConfiguration bucketConfiguration = new BucketConfiguration();

        bucketConfiguration.setRepairWorkerTimeout(Duration.standardSeconds(3));

        // dont delete repaired buckets since we need to query their status
        // for testing purposes
        bucketConfiguration.setDeleteBucketsAfterRepair(false);

        serviceConfiguration.setBucketConfiguration(bucketConfiguration);

        final Injector defaultInjector = getDefaultInjector(serviceConfiguration);

        final RepairWorkerFactory repairWorkerFactory = defaultInjector.getInstance(RepairWorkerFactory.class);

        final QueueName queueName = QueueName.valueOf("repairer_republishes_newly_visible_in_tombstoned_bucket");

        final QueueDefinition queueDefinition = setupQueue(queueName, 1);

        repairWorkerFactory.forQueue(queueDefinition);

        final DataContextFactory contextFactory = defaultInjector.getInstance(DataContextFactory.class);

        final DataContext dataContext = contextFactory.forQueue(queueDefinition);

        final MonotonicIndex index = MonotonicIndex.valueOf(0);

        final Message message = Message.builder()
                                       .blob("BOO!")
                                       .index(index)
                                       .tag(MessageTag.random())
                                       .build();

        final RepairWorkerImpl repairWorker = (RepairWorkerImpl) repairWorkerFactory.forQueue(queueDefinition);

        repairWorker.start();

        dataContext.getMessageRepository().putMessage(message);

        getTestClock().tick();

        dataContext.getMessageRepository().tombstone(ReaderBucketPointer.valueOf(0));

        getTestClock().tickSeconds(5L);

        repairWorker.waitForNextRun();

        final Message repairedMessage = dataContext.getMessageRepository().getMessage(index);

        assertThat(repairedMessage.isAcked()).isTrue();

        final Message republish = dataContext.getMessageRepository().getMessage(MonotonicIndex.valueOf(1));

        assertThat(republish.getBlob()).isEqualTo(repairedMessage.getBlob());

        repairWorker.stop();
    }

    @Test
    public void repairer_moves_off_ghost_messages() throws InterruptedException, ExistingMonotonFoundException {

        final ServiceConfiguration serviceConfiguration = new ServiceConfiguration();

        final BucketConfiguration bucketConfiguration = new BucketConfiguration();

        bucketConfiguration.setRepairWorkerTimeout(Duration.standardSeconds(10));

        serviceConfiguration.setBucketConfiguration(bucketConfiguration);

        final Injector defaultInjector = getDefaultInjector(serviceConfiguration);

        final RepairWorkerFactory repairWorkerFactory = defaultInjector.getInstance(RepairWorkerFactory.class);

        final QueueName queueName = QueueName.valueOf("repairer_moves_off_ghost_messages");

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        repairWorkerFactory.forQueue(queueDefinition);

        final DataContextFactory contextFactory = defaultInjector.getInstance(DataContextFactory.class);

        final DataContext dataContext = contextFactory.forQueue(queueDefinition);

        MonotonicIndex index = dataContext.getMonotonicRepository().nextMonotonic();

        Message message = Message.builder()
                                 .blob("BOO!")
                                 .index(index)
                                 .build();

        final RepairWorkerImpl repairWorker = (RepairWorkerImpl) repairWorkerFactory.forQueue(queueDefinition);

        RepairBucketPointer repairCurrentBucketPointer = dataContext.getPointerRepository().getRepairCurrentBucketPointer();

        assertThat(repairCurrentBucketPointer.get()).isEqualTo(0);

        dataContext.getMessageRepository().putMessage(message);

        getTestClock().tick();

        message = dataContext.getMessageRepository().getMessage(index);

        dataContext.getMessageRepository().ackMessage(message);

        repairWorker.start();

        // the ghost message by claiming a monoton that wont ever appear
        dataContext.getMonotonicRepository().nextMonotonic();
        getTestClock().tick();

        // tombstone the old bucket
        dataContext.getMessageRepository().tombstone(ReaderBucketPointer.valueOf(0));

        index = dataContext.getMonotonicRepository().nextMonotonic();

        final Message thirdmessage = Message.builder().blob("3rd").index(index).build();

        dataContext.getMessageRepository().putMessage(thirdmessage);

        getTestClock().tickSeconds(50L);

        repairWorker.waitForNextRun();

        // assert that the repair pointer moved
        repairCurrentBucketPointer = dataContext.getPointerRepository().getRepairCurrentBucketPointer();

        assertThat(repairCurrentBucketPointer.get()).isEqualTo(1);

        repairWorker.stop();
    }

    @Test
    public void repair_manager_adds_new_workers() throws Exception {
        final Injector defaultInjector = getDefaultInjector(new ServiceConfiguration(), CqlDb.createFresh());

        final RepairWorkerManager manager = defaultInjector.getInstance(RepairWorkerManager.class);

        final QueueName queueName = QueueName.valueOf("repair_manager_adds_new_workers");

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        final QueueRepository contextFactory = defaultInjector.getInstance(QueueRepository.class);

        manager.refresh();

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(1);

        contextFactory.tryDeleteQueueDefinition(queueDefinition);

        manager.refresh();

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(0);
    }

    @Test
    public void repair_manager_properly_keeps_track_of_existing_workers() throws Exception {
        final Injector defaultInjector = getDefaultInjector(new ServiceConfiguration(), CqlDb.createFresh());

        final RepairWorkerManager manager = defaultInjector.getInstance(RepairWorkerManager.class);

        final QueueName queueName = QueueName.valueOf("repair_manager_properly_keeps_track_of_existing_workers");

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        final QueueRepository contextFactory = defaultInjector.getInstance(QueueRepository.class);

        // refreshing twice should not add or remove anyone since no queues were added/deleted
        manager.refresh();
        manager.refresh();

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(1);

        contextFactory.tryDeleteQueueDefinition(queueDefinition);

        manager.refresh();
        manager.refresh();

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(0);
    }
}
