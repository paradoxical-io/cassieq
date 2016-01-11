package io.paradoxical.cassieq.model;

import com.datastax.driver.core.Row;
import io.paradoxical.cassieq.dataAccess.Tables;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueueDefinition {
    private AccountName accountName;
    private QueueName queueName;
    private final BucketSize bucketSize;
    private final Integer maxDeliveryCount;
    private final QueueStatus status;
    private final int version;
    private final Integer repairWorkerPollFrequencySeconds;
    private final Integer repairWorkerTombstonedBucketTimeoutSeconds;
    private final Boolean deleteBucketsAfterFinaliziation;

    public QueueId getId() {
        return QueueId.valueOf(accountName, queueName, version);
    }

    public QueueDefinition(
            final AccountName accountName,
            final QueueName queueName,
            final BucketSize bucketSize,
            final Integer maxDeliveryCount,
            final QueueStatus status,
            final Integer version,
            final Integer repairWorkerPollFrequencySeconds,
            final Integer repairWorkerTombstonedBucketTimeoutSeconds,
            final Boolean deleteBucketsAfterFinaliziation) {
        this.accountName = accountName;
        this.queueName = queueName;
        this.deleteBucketsAfterFinaliziation = deleteBucketsAfterFinaliziation == null ? true : deleteBucketsAfterFinaliziation;
        this.version = version == null ? 0 : version;
        this.bucketSize = bucketSize == null ? BucketSize.valueOf(20) : bucketSize;
        this.maxDeliveryCount = maxDeliveryCount == null ? 5 : maxDeliveryCount;
        this.status = status == null ? QueueStatus.Active : status;
        this.repairWorkerPollFrequencySeconds = repairWorkerPollFrequencySeconds == null ? 5 : repairWorkerPollFrequencySeconds;
        this.repairWorkerTombstonedBucketTimeoutSeconds = repairWorkerTombstonedBucketTimeoutSeconds == null ? 15 : repairWorkerTombstonedBucketTimeoutSeconds;
    }

    public static QueueDefinition fromRow(final Row row) {
        return QueueDefinition.builder()
                              .bucketSize(BucketSize.valueOf(row.getInt(Tables.Queue.BUCKET_SIZE)))
                              .maxDeliveryCount(row.getInt(Tables.Queue.MAX_DELIVERY_COUNT))
                              .status(QueueStatus.values()[row.getInt(Tables.Queue.STATUS)])
                              .queueName(QueueName.valueOf(row.getString(Tables.Queue.QUEUE_NAME)))
                              .accountName(AccountName.valueOf(row.getString(Tables.Queue.ACCOUNT_NAME)))
                              .version(row.getInt(Tables.Queue.VERSION))
                              .repairWorkerPollFrequencySeconds(row.getInt(Tables.Queue.REPAIR_WORKER_POLL_FREQ_SECONDS))
                              .repairWorkerTombstonedBucketTimeoutSeconds(row.getInt(Tables.Queue.REPAIR_WORKER_TOMBSTONE_BUCKET_TIMEOUT_SECONDS))
                              .deleteBucketsAfterFinaliziation(row.getBool(Tables.Queue.DELETE_BUCKETS_AFTER_FINALIZATION))
                              .build();
    }
}
