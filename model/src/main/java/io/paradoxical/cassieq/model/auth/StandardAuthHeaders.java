package io.paradoxical.cassieq.model.auth;

import lombok.Getter;

public enum StandardAuthHeaders {
    RequestTime("x-cassieq-request-time");

    @Getter
    private final String headerName;

    StandardAuthHeaders(final String headerName) {

        this.headerName = headerName;
    }
}
