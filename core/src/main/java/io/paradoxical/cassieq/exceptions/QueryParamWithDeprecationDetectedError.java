package io.paradoxical.cassieq.exceptions;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

public class QueryParamWithDeprecationDetectedError extends BadRequestException {
    public QueryParamWithDeprecationDetectedError(final ErrorEntity entity) {
        super(Response.status(Response.Status.BAD_REQUEST)
                      .entity(entity)
                      .build());
    }
}
