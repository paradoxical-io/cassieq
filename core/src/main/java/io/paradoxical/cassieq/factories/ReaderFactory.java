package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.workers.reader.Reader;

public interface ReaderFactory {
    Reader forQueue(AccountName accountName, QueueDefinition definition);
}
