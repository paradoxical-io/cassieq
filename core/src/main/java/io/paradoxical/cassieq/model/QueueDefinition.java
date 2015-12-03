package io.paradoxical.cassieq.model;

import com.datastax.driver.core.Row;
import io.paradoxical.cassieq.dataAccess.Tables;
import io.paradoxical.cassieq.model.QueueName;
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
    private final Integer bucketSize;
    private final Integer maxDeliveryCount;

    private QueueDefinition(QueueName queueName, Integer bucketSize, Integer maxDeliveryCount) {
        this.queueName = queueName;
        this.bucketSize = bucketSize == null ? 20 : bucketSize;
        this.maxDeliveryCount = maxDeliveryCount == null ? 5 : maxDeliveryCount;
    }

    public static QueueDefinition fromRow(final Row row) {
        return QueueDefinition.builder()
                              .bucketSize(row.getInt(Tables.Queue.BUCKET_SIZE))
                              .maxDeliveryCount(row.getInt(Tables.Queue.MAX_DEQUEUE_COUNT))
                              .queueName(QueueName.valueOf(row.getString(Tables.Queue.QUEUENAME)))
                              .build();
    }
}
