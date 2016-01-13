package io.paradoxical.cassieq.discoverable.resources.api.v1;

import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.AccessLevel;
import lombok.Getter;

import javax.ws.rs.PathParam;
import java.util.Optional;

public abstract class BaseQueueResource extends BaseAccountResource {

    @Getter(AccessLevel.PROTECTED)
    private final ReaderFactory readerFactory;

    @Getter(AccessLevel.PROTECTED)
    private final MessageRepoFactory messageRepoFactory;

    @Getter(AccessLevel.PROTECTED)
    private final MonotonicRepoFactory monotonicRepoFactory;

    @Getter(AccessLevel.PROTECTED)
    private final QueueRepository queueRepository;

    protected BaseQueueResource(
            ReaderFactory readerFactory,
            MessageRepoFactory messageRepoFactory,
            MonotonicRepoFactory monotonicRepoFactory,
            QueueRepository queueRepository,
            @PathParam("accountName") final AccountName accountName) {
        super(accountName);

        this.readerFactory = readerFactory;
        this.messageRepoFactory = messageRepoFactory;
        this.monotonicRepoFactory = monotonicRepoFactory;
        this.queueRepository = queueRepository;
    }

    protected Optional<QueueDefinition> getQueueDefinition(final QueueName queueName) {
        return queueRepository.getActiveQueue(queueName);
    }


}
