package io.paradoxical.cassieq.model;

import com.datastax.driver.core.Row;
import io.paradoxical.cassieq.dataAccess.Tables;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class QueueDefinition {
    @NotNull
    @NonNull
    private final QueueName queueName;
    private final BucketSize bucketSize;
    private final Integer maxDeliveryCount;

    private QueueDefinition(QueueName queueName, BucketSize bucketSize, Integer maxDeliveryCount) {
        this.queueName = queueName;
        this.bucketSize = bucketSize == null ? BucketSize.valueOf(20) : bucketSize;
        this.maxDeliveryCount = maxDeliveryCount == null ? 5 : maxDeliveryCount;
    }

    private QueueDefinition(QueueName queueName, Integer bucketSize, Integer maxDeliveryCount) {
        this(queueName, BucketSize.valueOf(bucketSize), maxDeliveryCount);
    }

    public static QueueDefinition fromRow(final Row row) {
        return QueueDefinition.builder()
                              .bucketSize(BucketSize.valueOf(row.getInt(Tables.Queue.BUCKET_SIZE)))
                              .maxDeliveryCount(row.getInt(Tables.Queue.MAX_DEQUEUE_COUNT))
                              .queueName(QueueName.valueOf(row.getString(Tables.Queue.QUEUENAME)))
                              .build();
    }
}
