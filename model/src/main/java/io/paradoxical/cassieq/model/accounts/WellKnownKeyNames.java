package io.paradoxical.cassieq.model.accounts;

import lombok.Getter;

public enum WellKnownKeyNames {
    Primary(KeyName.valueOf("primary")),
    Secondary(KeyName.valueOf("secondary"));

    @Getter
    private final KeyName keyName;

    WellKnownKeyNames(final KeyName keyName) {

        this.keyName = keyName;
    }
}
