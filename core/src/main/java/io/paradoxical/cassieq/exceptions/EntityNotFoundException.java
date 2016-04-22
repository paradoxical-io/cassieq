package io.paradoxical.cassieq.exceptions;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

public abstract class EntityNotFoundException extends NotFoundException {
    public EntityNotFoundException(final ErrorEntity errorEntity) {
        super(Response.status(Response.Status.NOT_FOUND)
                      .entity(errorEntity)
                      .build());
    }
}

