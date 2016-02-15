package io.paradoxical.cassieq.exceptions;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

public class ConflictException extends ClientErrorException {

    public ConflictException(final String operation, final String messageFormat, Object ...args) {
        super(Response.status(Response.Status.CONFLICT)
                      .entity(new ErrorEntity(ErrorReason.Conflict, operation, messageFormat, args))
                      .build());
    }
}
