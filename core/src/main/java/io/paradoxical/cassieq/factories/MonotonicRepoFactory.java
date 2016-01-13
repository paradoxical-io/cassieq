package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.accounts.AccountName;

public interface MonotonicRepoFactory {
    MonotonicRepository forQueue(QueueId queueId);
}

