package io.paradoxical.cassieq.workers;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.DeletionJob;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueAlreadyDeletingException;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.MessageDeleterJobProcessorFactory;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;

import java.util.Optional;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class QueueDeleter {
    private static final Logger logger = getLogger(QueueDeleter.class);

    private final DataContextFactory dataContextFactory;
    private final MessageDeleterJobProcessorFactory messageDeleterJobProcessorFactory;
    private final RepairWorkerManager repairWorkerManager;
    private final AccountName accountName;

    @Inject
    public QueueDeleter(
            DataContextFactory dataContextFactory,
            MessageDeleterJobProcessorFactory messageDeleterJobProcessorFactory,
            RepairWorkerManager repairWorkerManager,
            @Assisted AccountName accountName) {
        this.dataContextFactory = dataContextFactory;
        this.messageDeleterJobProcessorFactory = messageDeleterJobProcessorFactory;
        this.repairWorkerManager = repairWorkerManager;
        this.accountName = accountName;
    }

    public void delete(QueueName queueName) throws QueueAlreadyDeletingException {
        final QueueRepository queueRepository = dataContextFactory.forAccount(accountName);

        final Optional<QueueDefinition> optionalDefinition = queueRepository.getActiveQueue(queueName);

        if (!optionalDefinition.isPresent()) {
            return;
        }


        final QueueDefinition queueDefinition = optionalDefinition.get();

        logger.with(queueDefinition).debug("Attempting to delete queue");

        // This should be the first thing. this way the pointers below can't be modified further.
        final Optional<DeletionJob> deletionJob = queueRepository.tryMarkForDeletion(queueDefinition);

        if (!deletionJob.isPresent()) {
            throw new QueueAlreadyDeletingException(queueDefinition);
        }

        // delegate actual work to another processor that handles the job
        // this can be used to spin up deletion jobs after the fact
        messageDeleterJobProcessorFactory.createDeletionProcessor(deletionJob.get()).start();

        repairWorkerManager.notifyChanges();
    }

    public interface Factory {
        QueueDeleter create(AccountName accountName);
    }
}
