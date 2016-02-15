package io.paradoxical.cassieq.exceptions;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

public class InternalSeverError extends ServerErrorException {
    public InternalSeverError(final String operation, final Throwable error) {
        this(new ErrorEntity(operation, error.getMessage()), error);
    }

    protected InternalSeverError(final ErrorEntity entity, final Throwable error) {
        super(Response.serverError()
                      .entity(entity)
                      .build(),
              error);
    }
}

