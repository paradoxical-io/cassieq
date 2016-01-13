package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.model.accounts.AccountName;

public interface QueueRepositoryFactory {
    QueueRepository forAccount(AccountName accountName);
}
