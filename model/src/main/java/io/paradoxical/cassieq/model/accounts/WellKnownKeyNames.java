package io.paradoxical.cassieq.model.accounts;

import lombok.Getter;

public enum WellKnownKeyNames {
    Primary("primary"),
    Secondary("secondary");

    @Getter
    private final String keyName;

    WellKnownKeyNames(final String keyName) {

        this.keyName = keyName;
    }
}
