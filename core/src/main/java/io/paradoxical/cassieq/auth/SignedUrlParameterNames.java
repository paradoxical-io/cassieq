package io.paradoxical.cassieq.auth;

import lombok.Getter;
import lombok.NonNull;

public enum SignedUrlParameterNames {
    AuthorizationLevels("auth"),
    Signature("sig");

    @Getter
    private final String parameterName;

    SignedUrlParameterNames(@NonNull final String parameterName) {
        this.parameterName = parameterName;
    }
}
