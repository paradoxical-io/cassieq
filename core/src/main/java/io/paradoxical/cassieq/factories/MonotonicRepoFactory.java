package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.model.QueueName;

public interface MonotonicRepoFactory{
    MonotonicRepository forQueue(QueueName queueName);
}
