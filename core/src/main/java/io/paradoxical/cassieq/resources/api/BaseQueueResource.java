package io.paradoxical.cassieq.resources.api;

import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.exceptions.QueueInternalServerError;
import io.paradoxical.cassieq.exceptions.QueueNotFoundException;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.resources.api.BaseAccountResource;
import javaslang.control.Try;
import lombok.AccessLevel;
import lombok.Getter;

import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
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

    @Context
    private ResourceInfo resourceContext;

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

    protected QueueDefinition lookupQueueDefinition(final QueueName queueName)
            throws QueueInternalServerError, QueueNotFoundException {

        final String resourceMethodName = resourceContext.getResourceMethod().getName();

        final Optional<QueueDefinition> queueDefinitionOption =
                Try.of(() -> queueRepository.getActiveQueue(queueName))
                   .orElseThrow(error -> new QueueInternalServerError(resourceMethodName, queueName, error));

        return queueDefinitionOption.orElseThrow(() -> new QueueNotFoundException(resourceMethodName, queueName));
    }


}
