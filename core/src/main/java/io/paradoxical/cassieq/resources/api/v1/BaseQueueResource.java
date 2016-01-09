package io.paradoxical.cassieq.resources.api.v1;

import com.godaddy.logging.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.godaddy.logging.LoggerFactory.getLogger;

public abstract class BaseQueueResource {

    private static final Logger logger = getLogger(BaseQueueResource.class);

    @Getter(AccessLevel.PROTECTED)
    private final ReaderFactory readerFactory;

    @Getter(AccessLevel.PROTECTED)
    private final MessageRepoFactory messageRepoFactory;

    @Getter(AccessLevel.PROTECTED)
    private final MonotonicRepoFactory monotonicRepoFactory;

    @Getter(AccessLevel.PROTECTED)
    private final QueueRepository queueRepository;

    private final Cache<QueueName, Optional<QueueDefinition>> queueDefinitionCache;

    protected BaseQueueResource(
            ReaderFactory readerFactory,
            MessageRepoFactory messageRepoFactory,
            MonotonicRepoFactory monotonicRepoFactory,
            QueueRepository queueRepository) {
        this.readerFactory = readerFactory;
        this.messageRepoFactory = messageRepoFactory;
        this.monotonicRepoFactory = monotonicRepoFactory;
        this.queueRepository = queueRepository;

        queueDefinitionCache = CacheBuilder.newBuilder()
                                           .maximumSize(10000).build();
    }

    protected Optional<QueueDefinition> getQueueDefinition(@NonNull final QueueName queueName) {
        try {
            return queueDefinitionCache.get(queueName, () -> queueRepository.getActiveQueue(queueName));
        }
        catch (ExecutionException e) {
            logger.error(e, "Error");
            return queueRepository.getActiveQueue(queueName);
        }
    }

    protected void invalidateQueueDefinitionCache(@NonNull final QueueName queueName) {
        queueDefinitionCache.invalidate(queueName);
    }

    protected void addToQueueCache(@NonNull QueueName queueName, @NonNull QueueDefinition queueDefinition) {
        queueDefinitionCache.put(queueName, Optional.of(queueDefinition));
    }

    protected Response buildQueueNotFoundResponse(final QueueName queue) {
        return Response.status(Response.Status.NOT_FOUND).entity(new Object() {
            @Getter
            private final String result = "not-found";

            @Getter
            private final String queueName = queue.get();
        }).build();
    }


    protected Response buildConflictResponse(String reason) {
        return Response.status(Response.Status.CONFLICT)
                       .entity(new Object() {
                           public final String result = "conflict";

                           public final String message = reason;
                       })
                       .build();
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
