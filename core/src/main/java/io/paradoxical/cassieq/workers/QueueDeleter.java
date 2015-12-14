package io.paradoxical.cassieq.workers;

import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.DeletionJob;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueAlreadyDeletingException;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContext;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.MessagePointer;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatus;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;

import java.util.Optional;

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

    public void delete(QueueName queueName) throws QueueAlreadyDeletingException {
        final QueueDefinition queueDefinition = factory.getDefinition(queueName).get();

        // This should be the first thing. this way the pointers below can't be modified further.
        final Optional<DeletionJob> deletionJob = queueRepository.tryMarkForDeletion(queueDefinition);

        if(!deletionJob.isPresent()) {
            throw new QueueAlreadyDeletingException(queueDefinition);
        }

        final DataContext dataContext = factory.forQueue(queueDefinition);

        final MessagePointer startPointer = getMinStartPointer(dataContext, queueDefinition);

        final MessagePointer endPointer = dataContext.getMonotonicRepository().getCurrent();

        dataContext.getMessageRepository().deleteAllMessages(startPointer, endPointer);

        dataContext.getMonotonicRepository().deleteAll();

        dataContext.getPointerRepository().deleteAll();

        // queue definition is now totally inactive
        queueRepository.tryAdvanceQueueStatus(queueName, QueueStatus.Inactive);

        queueRepository.deleteCompletionJob(deletionJob.get());

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
