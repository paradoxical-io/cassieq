package io.paradoxical.cassieq.workers;

import com.google.inject.Inject;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.MessagePointer;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueStatus;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;

public class QueueDeleter {
    private final DataContextFactory factory;
    private final RepairWorkerManager repairWorkerManager;

    @Inject
    public QueueDeleter(DataContextFactory factory, RepairWorkerManager repairWorkerManager) {
        this.factory = factory;
        this.repairWorkerManager = repairWorkerManager;
    }

    public void delete(QueueDefinition definition) {
        final DataContext dataContext = factory.forQueue(definition);

        final MessagePointer startPointer = getMinStartPointer(dataContext, definition);

        final MessagePointer endPointer = dataContext.getMonotonicRepository().getCurrent();

        dataContext.getQueueRepository().setQueueStatus(definition, QueueStatus.Deleting);

        dataContext.getMessageRepository().deleteAllMessages(startPointer, endPointer);

        dataContext.getMonotonicRepository().deleteAll();

        dataContext.getPointerRepository().deleteAll();

        // actally delete the queue definition
        dataContext.getQueueRepository().deleteQueueDefinition(definition.getQueueName());

        repairWorkerManager.refresh();
    }

    private MessagePointer getMinStartPointer(DataContext dataContext, QueueDefinition queueDefinition) {
        final MonotonicIndex repairPointer = dataContext.getPointerRepository().getRepairCurrentBucketPointer().startOf(queueDefinition.getBucketSize());

        final InvisibilityMessagePointer currentInvisPointer = dataContext.getPointerRepository().getCurrentInvisPointer();

        if(repairPointer.get() < currentInvisPointer.get()){
            return repairPointer;
        }

        return currentInvisPointer;
    }
}
