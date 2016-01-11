package io.paradoxical.cassieq.unittests;

import categories.BuildVerification;
import com.google.inject.Injector;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.QueueDataContext;
import io.paradoxical.cassieq.factories.QueueRepositoryFactory;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MessageTag;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import io.paradoxical.cassieq.unittests.data.CqlDb;
import io.paradoxical.cassieq.workers.repair.RepairWorkerImpl;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;
import io.paradoxical.cassieq.workers.repair.SimpleRepairWorkerManager;
import lombok.Cleanup;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class RepairTests extends TestBase {
    @Test
    public void repairer_republishes_newly_visible_in_tombstoned_bucket() throws InterruptedException, ExistingMonotonFoundException, ExecutionException {

        final ServiceConfiguration serviceConfiguration = new ServiceConfiguration();

        final Injector defaultInjector = getDefaultInjector(serviceConfiguration);

        final RepairWorkerFactory repairWorkerFactory = defaultInjector.getInstance(RepairWorkerFactory.class);

        final QueueName queueName = QueueName.valueOf("repairer_republishes_newly_visible_in_tombstoned_bucket");

        final QueueDefinition queueDefinition = QueueDefinition.builder()
                                                               .queueName(queueName)
                                                               .bucketSize(BucketSize.valueOf(1))
                                                               .repairWorkerPollFrequencySeconds(1)
                                                               .repairWorkerTombstonedBucketTimeoutSeconds(3)
                                                               // dont delete since we need to query after
                                                               .deleteBucketsAfterFinaliziation(false)
                                                               .build();

        createQueue(queueDefinition);

        repairWorkerFactory.forQueue(queueDefinition);

        final DataContextFactory contextFactory = defaultInjector.getInstance(DataContextFactory.class);

        final QueueDataContext dataContext = contextFactory.forQueue(queueDefinition);
        final Message message = Message.builder()
                                       .blob("BOO!")
                                       .index(dataContext.getMonotonicRepository().nextMonotonic())
                                       .tag(MessageTag.random())
                                       .build();

        final RepairWorkerImpl repairWorker = (RepairWorkerImpl) repairWorkerFactory.forQueue(queueDefinition);

        repairWorker.start();

        dataContext.getMessageRepository().putMessage(message);

        getTestClock().tick();

        dataContext.getMessageRepository().tombstone(ReaderBucketPointer.valueOf(0));

        getTestClock().tickSeconds(5L);

        repairWorker.waitForNextRun();

        final Message repairedMessage = dataContext.getMessageRepository().getMessage(message.getIndex());

        assertThat(repairedMessage.isAcked()).isTrue();

        final Message republish = dataContext.getMessageRepository().getMessage(MonotonicIndex.valueOf(1));

        assertThat(republish.getBlob()).isEqualTo(repairedMessage.getBlob());

        repairWorker.stop();
    }

    @Test
    public void repairer_moves_off_ghost_messages() throws InterruptedException, ExistingMonotonFoundException {

        final ServiceConfiguration serviceConfiguration = new ServiceConfiguration();

        final Injector defaultInjector = getDefaultInjector(serviceConfiguration);

        final RepairWorkerFactory repairWorkerFactory = defaultInjector.getInstance(RepairWorkerFactory.class);

        final QueueName queueName = QueueName.valueOf("repairer_moves_off_ghost_messages");

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        repairWorkerFactory.forQueue(queueDefinition);

        final DataContextFactory contextFactory = defaultInjector.getInstance(DataContextFactory.class);

        final QueueDataContext dataContext = contextFactory.forQueue(queueDefinition);

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

        @Cleanup("stop") final RepairWorkerManager manager = defaultInjector.getInstance(RepairWorkerManager.class);

        manager.start();

        final QueueName queueName = QueueName.valueOf("repair_manager_adds_new_workers");

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        final DataContextFactory dataContextFactory = defaultInjector.getInstance(DataContextFactory.class);

        manager.notifyChanges();

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(1);

        dataContextFactory.forAccount(testAccountName).tryMarkForDeletion(queueDefinition);

        manager.notifyChanges();

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(0);
    }

    @Test
    public void repair_manager_properly_keeps_track_of_existing_workers() throws Exception {
        final Injector defaultInjector = getDefaultInjector(new ServiceConfiguration(), CqlDb.createFresh());

        @Cleanup("stop") final RepairWorkerManager manager = defaultInjector.getInstance(RepairWorkerManager.class);

        manager.start();

        final QueueName queueName = QueueName.valueOf("repair_manager_properly_keeps_track_of_existing_workers");

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        final DataContextFactory contextFactory = defaultInjector.getInstance(DataContextFactory.class);

        // refreshing twice should not add or remove anyone since no queues were added/deleted
        manager.notifyChanges();
        manager.notifyChanges();

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(1);

        contextFactory.forAccount(testAccountName).tryMarkForDeletion(queueDefinition);

        manager.notifyChanges();
        manager.notifyChanges();

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(0);
    }

    @Test
    public void repair_manager_polls_queues_for_new_workers() throws Exception {
        final ServiceConfiguration configuration = new ServiceConfiguration();

        configuration.getRepairConf().setManagerRefreshRateSeconds(1);

        final Injector defaultInjector = getDefaultInjector(configuration, CqlDb.createFresh());

        @Cleanup("stop") final RepairWorkerManager manager = defaultInjector.getInstance(RepairWorkerManager.class);

        final QueueName queueName = QueueName.valueOf("repair_manager_polls_queues_for_new_workers");

        manager.start();

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        final QueueRepositoryFactory queueRepositoryFactory = defaultInjector.getInstance(QueueRepositoryFactory.class);

        Thread.sleep(2000);

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(1);

        final QueueRepository queueRepository = queueRepositoryFactory.forAccount(testAccountName);
        queueRepository.tryMarkForDeletion(queueDefinition);

        Thread.sleep(2000);

        assertThat(((SimpleRepairWorkerManager) manager).getCurrentRepairWorkers().size()).isEqualTo(0);
    }
}
