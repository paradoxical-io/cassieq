package io.paradoxical.cassieq.dataAccess;

import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.QueueStatsId;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Data;

@Data
public class DeletionJob {
    private final AccountName accountName;

    private final QueueName queueName;

    private final QueueStatsId queueStatsId;

    private final int version;

    private final BucketSize bucketSize;

    public QueueId getQueueIdentifier() {
        return QueueId.valueOf(accountName, queueName, version);
    }

    public DeletionJob(QueueDefinition definition) {
        this.queueName = definition.getQueueName();

        this.version = definition.getVersion();

        this.bucketSize = definition.getBucketSize();

        this.accountName = definition.getAccountName();

        this.queueStatsId = definition.getQueueStatsId();
    }
}
