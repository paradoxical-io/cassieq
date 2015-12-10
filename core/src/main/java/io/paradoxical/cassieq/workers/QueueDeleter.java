package io.paradoxical.cassieq.workers;

import com.google.inject.Inject;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;

public class QueueDeleter {
    private final DataContextFactory factory;
    private final RepairWorkerManager repairWorkerManager;

    @Inject
    public QueueDeleter(DataContextFactory factory, RepairWorkerManager repairWorkerManager) {
        this.factory = factory;
        this.repairWorkerManager = repairWorkerManager;
    }

    public void delete(QueueDefinition queueName) {
        final DataContext dataContext = factory.forQueue(queueName);

        final MonotonicIndex from = dataContext.getPointerRepository().getRepairCurrentBucketPointer().startOf(queueName.getBucketSize());

        //dataContext.getMessageRepository().deleteAllMessages();

        dataContext.getMonotonicRepository().deleteAll();

        dataContext.getPointerRepository().deleteAll();

        dataContext.getQueueRepository().deleteQueue(queueName.getQueueName());

        repairWorkerManager.refresh();
    }
}
