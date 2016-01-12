package io.paradoxical.cassieq.unittests;

import com.datastax.driver.core.Session;
import com.godaddy.logging.Logger;
import com.google.inject.Injector;
import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MessageTag;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import io.paradoxical.cassieq.unittests.modules.HazelcastTestModule;
import io.paradoxical.cassieq.unittests.modules.InMemorySessionProvider;
import io.paradoxical.cassieq.unittests.server.SelfHostServer;
import io.paradoxical.cassieq.workers.repair.RepairWorkerImpl;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;
import io.paradoxical.cassieq.workers.repair.SimpleRepairWorkerManager;
import lombok.Cleanup;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static com.godaddy.logging.LoggerFactory.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

public class RepairTests extends TestBase {
    private static final Logger logger = getLogger(RepairTests.class);


    @Test
    public void repair_manager_claims_workers() throws Exception {
        Session session = CqlDb.createFresh();

        @Cleanup SelfHostServer server1 = new SelfHostServer(new InMemorySessionProvider(session), new HazelcastTestModule("repair_manager_claims_workers"));
        @Cleanup SelfHostServer server2 = new SelfHostServer(new InMemorySessionProvider(session), new HazelcastTestModule("repair_manager_claims_workers"));

        server1.start();

        final QueueCreateOptions createOptions = fixture.manufacturePojo(QueueCreateOptions.class);

        assertThat(server1.getClient().createQueue(createOptions).execute().isSuccess()).isTrue();

        final Injector server1Injector = server1.getService().getGuiceBundleProvider().getBundle().getInjector();

        final SimpleRepairWorkerManager managerInstance1 = (SimpleRepairWorkerManager) server1Injector.getInstance(RepairWorkerManager.class);

        managerInstance1.claim();

        assertThat(managerInstance1.getClaimedRepairWorkers().size()).isEqualTo(1);

        server2.start();

        final Injector server2Injector = server2.getService().getGuiceBundleProvider().getBundle().getInjector();

        final SimpleRepairWorkerManager managerInstance2 = (SimpleRepairWorkerManager) server2Injector.getInstance(RepairWorkerManager.class);

        managerInstance2.claim();

        // all repair workers should be claimed
        assertThat(managerInstance2.getClaimedRepairWorkers().size()).isEqualTo(0);

        // shut down the first server
        server1.stop();

        managerInstance2.claim();

        assertThat(managerInstance2.getClaimedRepairWorkers().size()).isEqualTo(1);
    }

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
                                                               .deleteBucketsAfterFinalization(false)
                                                               .build();

        createQueue(queueDefinition);

        repairWorkerFactory.forQueue(queueDefinition);

        final DataContextFactory contextFactory = defaultInjector.getInstance(DataContextFactory.class);

        final DataContext dataContext = contextFactory.forQueue(queueDefinition);
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
        final Injector defaultInjector = getDefaultInjector(new ServiceConfiguration());

        @Cleanup("stop") final RepairWorkerManager manager = defaultInjector.getInstance(RepairWorkerManager.class);

        manager.start();

        final QueueName queueName = QueueName.valueOf("repair_manager_adds_new_workers");

        final QueueRepository queueRepository = defaultInjector.getInstance(QueueRepository.class);

        final int currentQueueNum = queueRepository.getQueueNames().size();

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        manager.notifyChanges();

        assertThat(((SimpleRepairWorkerManager) manager).getClaimedRepairWorkers().size()).isEqualTo(currentQueueNum + 1);

        queueRepository.tryMarkForDeletion(queueDefinition);

        manager.notifyChanges();

        assertThat(((SimpleRepairWorkerManager) manager).getClaimedRepairWorkers().size()).isEqualTo(currentQueueNum);
    }

    @Test
    public void repair_manager_properly_keeps_track_of_existing_workers() throws Exception {
        final Injector defaultInjector = getDefaultInjector(new ServiceConfiguration());

        @Cleanup("stop") final RepairWorkerManager manager = defaultInjector.getInstance(RepairWorkerManager.class);

        manager.start();

        final QueueName queueName = QueueName.valueOf("repair_manager_properly_keeps_track_of_existing_workers");

        final QueueRepository queueRepository = defaultInjector.getInstance(QueueRepository.class);

        final int currentQueueNum = queueRepository.getQueueNames().size();

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        // refreshing twice should not add or remove anyone since no queues were added/deleted
        manager.notifyChanges();
        manager.notifyChanges();

        assertThat(((SimpleRepairWorkerManager) manager).getClaimedRepairWorkers().size()).isEqualTo(currentQueueNum + 1);

        queueRepository.tryMarkForDeletion(queueDefinition);

        manager.notifyChanges();
        manager.notifyChanges();

        assertThat(((SimpleRepairWorkerManager) manager).getClaimedRepairWorkers().size()).isEqualTo(currentQueueNum);
    }

    @Test
    public void repair_manager_polls_queues_for_new_workers() throws Exception {
        final ServiceConfiguration configuration = new ServiceConfiguration();

        configuration.getRepairConf().setManagerRefreshRateSeconds(1);

        final Injector defaultInjector = getDefaultInjector(configuration);

        @Cleanup("stop") final RepairWorkerManager manager = defaultInjector.getInstance(RepairWorkerManager.class);

        final QueueName queueName = QueueName.valueOf("repair_manager_polls_queues_for_new_workers");

        manager.start();

        final QueueRepository queueRepository = defaultInjector.getInstance(QueueRepository.class);

        final int currentQueueNum = queueRepository.getQueueNames().size();

        final QueueDefinition queueDefinition = setupQueue(queueName, 2);

        Thread.sleep(500);

        assertThat(((SimpleRepairWorkerManager) manager).getClaimedRepairWorkers().size()).isEqualTo(currentQueueNum + 1);

        queueRepository.tryMarkForDeletion(queueDefinition);

        Thread.sleep(500);

        assertThat(((SimpleRepairWorkerManager) manager).getClaimedRepairWorkers().size()).isEqualTo(currentQueueNum);
    }
}
