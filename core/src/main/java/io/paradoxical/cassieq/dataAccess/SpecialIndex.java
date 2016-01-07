package io.paradoxical.cassieq.dataAccess;

import io.paradoxical.cassieq.model.MonotonicIndex;
import lombok.Getter;

public enum SpecialIndex {
    Finalizer(MonotonicIndex.valueOf(-3)),
    Tombstone(MonotonicIndex.valueOf(-2));

    @Getter
    private final MonotonicIndex index;

    SpecialIndex(final MonotonicIndex monotonicIndex) {
        index = monotonicIndex;
    }
}