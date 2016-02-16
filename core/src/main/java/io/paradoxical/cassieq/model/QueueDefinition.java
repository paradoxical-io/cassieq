package io.paradoxical.cassieq.model;

import com.datastax.driver.core.Row;
import io.paradoxical.cassieq.dataAccess.Tables;
import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.Optional;

@Data
@Builder(toBuilder = true)
public class QueueDefinition {
    private AccountName accountName;
    private QueueName queueName;
    private final BucketSize bucketSize;
    private final Integer maxDeliveryCount;
    private final QueueStatus status;
    private final int version;
    private final Integer repairWorkerPollFrequencySeconds;
    private final Integer repairWorkerTombstonedBucketTimeoutSeconds;
    private final Boolean deleteBucketsAfterFinalization;
    private final QueueStatsId queueStatsId;
    private final Optional<QueueName> dlqName;

    /**
     * Allow reading a queue's bucket messages in random order. Helps prevent reader collision
     */
    private final Boolean allowRandomBucketReading;

    public QueueId getId() {
        return QueueId.valueOf(accountName, queueName, version);
    }

    public QueueDefinition(
            @NonNull final AccountName accountName,
            @NonNull final QueueName queueName,
            final BucketSize bucketSize,
            final Integer maxDeliveryCount,
            final QueueStatus status,
            final Integer version,
            final Integer repairWorkerPollFrequencySeconds,
            final Integer repairWorkerTombstonedBucketTimeoutSeconds,
            final Boolean deleteBucketsAfterFinalization,
            final QueueStatsId queueStatsId,
            final Optional<QueueName> dlqName,
            final Boolean allowRandomBucketReading) {
        this.accountName = accountName;
        this.queueName = queueName;
        this.queueStatsId = queueStatsId;
        this.dlqName = dlqName == null ? Optional.empty() : dlqName;
        this.deleteBucketsAfterFinalization = deleteBucketsAfterFinalization == null ? true : deleteBucketsAfterFinalization;
        this.version = version == null ? 0 : version;
        this.bucketSize = bucketSize == null ? BucketSize.valueOf(20) : bucketSize;
        this.maxDeliveryCount = maxDeliveryCount == null ? 5 : maxDeliveryCount;
        this.status = status == null ? QueueStatus.Active : status;
        this.repairWorkerPollFrequencySeconds = repairWorkerPollFrequencySeconds == null ? 5 : repairWorkerPollFrequencySeconds;
        this.repairWorkerTombstonedBucketTimeoutSeconds = repairWorkerTombstonedBucketTimeoutSeconds == null ? 15 : repairWorkerTombstonedBucketTimeoutSeconds;
        this.allowRandomBucketReading = allowRandomBucketReading == null ? false : allowRandomBucketReading;
    }

    public static QueueDefinition fromRow(final Row row) {
        return QueueDefinition.builder()
                              .bucketSize(BucketSize.valueOf(row.getInt(Tables.Queue.BUCKET_SIZE)))
                              .maxDeliveryCount(row.getInt(Tables.Queue.MAX_DELIVERY_COUNT))
                              .status(QueueStatus.values()[row.getInt(Tables.Queue.STATUS)])
                              .queueName(QueueName.valueOf(row.getString(Tables.Queue.QUEUE_NAME)))
                              .accountName(AccountName.valueOf(row.getString(Tables.Queue.ACCOUNT_NAME)))
                              .version(row.getInt(Tables.Queue.VERSION))
                              .queueStatsId(QueueStatsId.valueOf(row.getString(Tables.Queue.QUEUE_STATS_ID)))
                              .repairWorkerPollFrequencySeconds(row.getInt(Tables.Queue.REPAIR_WORKER_POLL_FREQ_SECONDS))
                              .repairWorkerTombstonedBucketTimeoutSeconds(row.getInt(Tables.Queue.REPAIR_WORKER_TOMBSTONE_BUCKET_TIMEOUT_SECONDS))
                              .deleteBucketsAfterFinalization(row.getBool(Tables.Queue.DELETE_BUCKETS_AFTER_FINALIZATION))
                              .allowRandomBucketReading(row.getBool(Tables.Queue.ALLOW_RANDOM_BUCKET_READING))
                              .dlqName(getDlqName(row))
                              .build();
    }

    private static Optional<QueueName> getDlqName(final Row row) {
        final String string = row.getString(Tables.Queue.DLQ_NAME);

        if (string == null) {
            return Optional.empty();
        }

        return Optional.of(QueueName.valueOf(string));
    }
}
