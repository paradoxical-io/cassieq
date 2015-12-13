package io.paradoxical.cassieq.workers;

import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.MessagePointer;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;

public class QueueDeleter {
    private final DataContextFactory factory;
    private final QueueRepository queueRepository;
    private final RepairWorkerManager repairWorkerManager;

    @Inject
    public QueueDeleter(
            DataContextFactory factory,
            QueueRepository queueRepository,
            RepairWorkerManager repairWorkerManager) {
        this.factory = factory;
        this.queueRepository = queueRepository;
        this.repairWorkerManager = repairWorkerManager;
    }

    public void delete(QueueName queueName) {
        final QueueDefinition queueDefinition = factory.getDefinition(queueName).get();

        final DataContext dataContext = factory.forQueue(queueDefinition);

        final MessagePointer startPointer = getMinStartPointer(dataContext, queueDefinition);

        final MessagePointer endPointer = dataContext.getMonotonicRepository().getCurrent();

        queueRepository.markForDeletion(queueDefinition);

        dataContext.getMessageRepository().deleteAllMessages(startPointer, endPointer);

        dataContext.getMonotonicRepository().deleteAll();

        dataContext.getPointerRepository().deleteAll();

        // actally delete the queue definition
        queueRepository.tryDeleteQueueDefinition(queueDefinition);

        repairWorkerManager.refresh();
    }

    private MessagePointer getMinStartPointer(DataContext dataContext, QueueDefinition queueDefinition) {
        final MonotonicIndex repairPointer = dataContext.getPointerRepository().getRepairCurrentBucketPointer().startOf(queueDefinition.getBucketSize());

        final InvisibilityMessagePointer currentInvisPointer = dataContext.getPointerRepository().getCurrentInvisPointer();

        if (repairPointer.get() < currentInvisPointer.get()) {
            return repairPointer;
        }

        return currentInvisPointer;
    }
}
