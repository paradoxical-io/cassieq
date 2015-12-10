package io.paradoxical.cassieq.dataAccess.interfaces;

import io.paradoxical.cassieq.model.MonotonicIndex;

public interface MonotonicRepository {
    MonotonicIndex nextMonotonic();

    MonotonicIndex getCurrent();

    void deleteAll();
}
