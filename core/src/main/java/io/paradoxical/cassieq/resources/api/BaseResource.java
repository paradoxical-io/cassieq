package io.paradoxical.cassieq.resources.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.paradoxical.cassieq.model.QueueName;
import lombok.Getter;
import lombok.NonNull;

import javax.ws.rs.core.Response;

public abstract class BaseResource {
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

    protected Response buildErrorResponse(@NonNull final String operation, final QueueName queue, @NonNull final Exception e) {

        final String errorMessage = e.getMessage();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Object() {
            @Getter
            private final String result = "error";

            @Getter
            private final String op = operation;

            @Getter
            @JsonInclude(JsonInclude.Include.NON_NULL)
            private final QueueName queueName = queue;

            @Getter
            private final String message = errorMessage;
        }).build();
    }
}
