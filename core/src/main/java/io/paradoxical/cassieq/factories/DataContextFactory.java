package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;

import java.util.Optional;

public interface DataContextFactory {
    QueueDataContext forQueue(QueueDefinition definition);

    QueueRepository forAccount(AccountName accountName);

    AccountRepository getAccountRepository();

    MonotonicRepository getMonotonicRepository(QueueId queueId);

    PointerRepository getPointerRepository(QueueId queueId);
}

