package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.model.QueueId;

public interface MonotonicRepoFactory {
    MonotonicRepository forQueue(QueueId queueId);
}
