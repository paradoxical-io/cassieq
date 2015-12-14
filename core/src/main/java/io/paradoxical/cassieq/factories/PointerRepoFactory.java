package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.model.QueueId;

public interface PointerRepoFactory {
    PointerRepository forQueue(QueueId queueId);
}

