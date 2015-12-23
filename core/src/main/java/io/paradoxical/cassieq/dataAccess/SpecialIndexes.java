package io.paradoxical.cassieq.dataAccess;

import io.paradoxical.cassieq.model.MonotonicIndex;

public final class SpecialIndexes {
    public static final MonotonicIndex finalized = MonotonicIndex.valueOf(-3);

    public static final MonotonicIndex tombstone =   MonotonicIndex.valueOf(-2);
}