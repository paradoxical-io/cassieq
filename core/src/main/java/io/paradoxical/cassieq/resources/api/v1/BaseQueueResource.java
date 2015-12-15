package io.paradoxical.cassieq.resources.api.v1;

import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import lombok.AccessLevel;
import lombok.Getter;

import javax.ws.rs.core.Response;
import java.util.Optional;

public abstract class BaseQueueResource {

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
            QueueRepository queueRepository) {
        this.readerFactory = readerFactory;
        this.messageRepoFactory = messageRepoFactory;
        this.monotonicRepoFactory = monotonicRepoFactory;
        this.queueRepository = queueRepository;
    }

    protected Optional<QueueDefinition> getQueueDefinition(final QueueName queueName) {
        return queueRepository.getActiveQueue(queueName);
    }

    protected Response buildQueueNotFoundResponse(final QueueName queue) {
        return Response.status(Response.Status.NOT_FOUND).entity(new Object() {
            @Getter
            private final String result = "not-found";

            @Getter
            private final String queueName = queue.get();
        }).build();
    }

    protected Response buildErrorResponse(final String operation, final QueueName queue, final Exception e) {

        final String errorMessage = e.getMessage();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Object() {
            @Getter
            private final String result = "error";

            @Getter
            private final String op = operation;

            @Getter
            private final QueueName queueName = queue;

            @Getter
            private final String message = errorMessage;
        }).build();
    }
}
