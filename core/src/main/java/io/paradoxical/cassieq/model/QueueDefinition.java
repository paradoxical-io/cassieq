package io.paradoxical.cassieq.model;

import com.datastax.driver.core.Row;
import io.paradoxical.cassieq.dataAccess.Tables;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class QueueDefinition {
    private QueueName queueName;
    private final BucketSize bucketSize;
    private final Integer maxDeliveryCount;
    private final QueueStatus status;
    private final int version;

    public QueueId getId() {
        return QueueId.valueOf(queueName, version);
    }

    public QueueDefinition(
            QueueName queueName,
            BucketSize bucketSize,
            Integer maxDeliveryCount,
            QueueStatus status,
            Integer version) {
        this.queueName = queueName;
        this.version = version == null ? 0 : version;
        this.bucketSize = bucketSize == null ? BucketSize.valueOf(20) : bucketSize;
        this.maxDeliveryCount = maxDeliveryCount == null ? 5 : maxDeliveryCount;
        this.status = status == null ? QueueStatus.Active : status;
    }

    public static QueueDefinition fromRow(final Row row) {
        return QueueDefinition.builder()
                              .bucketSize(BucketSize.valueOf(row.getInt(Tables.Queue.BUCKET_SIZE)))
                              .maxDeliveryCount(row.getInt(Tables.Queue.MAX_DELIVERY_COUNT))
                              .status(QueueStatus.values()[row.getInt(Tables.Queue.STATUS)])
                              .queueName(QueueName.valueOf(row.getString(Tables.Queue.QUEUE_NAME)))
                              .version(row.getInt(Tables.Queue.VERSION))
                              .build();
    }
}
