package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.paradoxical.cassieq.model.validators.StringTypeValid;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder
public class QueueCreateOptions {
    private static final Integer DEFAULT_BUCKET_SIZE = 20;
    private static final Integer DEFAULT_MAX_DELIVERY_COUNT = 5;
    private static final Integer DEFAULT_REPAIR_POLL_SECONDS = 5;
    private static final Integer DEFAULT_REPAIR_POLL_BUCKET_FINALIZE_SECONDS = 15;
    private static final Integer DEFAULT_MAX_DELIVERY = 5;
    private static final Boolean DEFAULT_DELETE_BUCKETS_ON_FINALIZE = true;

    @StringTypeValid
    private final QueueName queueName;

    /**
     * The number of messages that go into a bucket.
     */
    private final Integer bucketSize;

    /**
     * Maximum number of times a message can be delivered on this queue
     */
    private final Integer maxDeliveryCount;

    /**
     * How long the repair worker checks the current bucket for tombstoning
     */
    private final Integer repairWorkerPollSeconds;

    /**
     * When a bucket is tombstoned, how long we will wait for delayed messages before finalizing the bucket
     */
    private final Integer repairWorkerBucketFinalizeTimeSeconds;

    /**
     * After finalization, if the bucket is _full_ and all acked whether or not to delete it
     * Unfull buckets will not be deleted
     */
    private final Boolean deleteBucketsAfterFinalize;

    /**
     * An optional DLQ linked to this queue. When messages reach their max delivery
     * count, they will be re-published to this queue. If no DLQ is specified, the messages
     * are discarded after max delivery.
     */
    private final Optional<QueueName> dlqName;

    public QueueCreateOptions(QueueName queueName) {
        this(queueName,
             DEFAULT_BUCKET_SIZE,
             DEFAULT_MAX_DELIVERY_COUNT,
             DEFAULT_REPAIR_POLL_SECONDS,
             DEFAULT_REPAIR_POLL_BUCKET_FINALIZE_SECONDS,
             DEFAULT_DELETE_BUCKETS_ON_FINALIZE,
             Optional.empty());
    }

    @JsonCreator
    public QueueCreateOptions(
            @JsonProperty("queueName") QueueName queueName,
            @JsonProperty("bucketSize") Integer bucketSize,
            @JsonProperty("maxDeliveryCount") Integer maxDeliveryCount,
            @JsonProperty("repairWorkerPollSeconds") Integer repairWorkerPollSeconds,
            @JsonProperty("repairWorkerBucketFinalizeTimeSeconds") Integer repairWorkerBucketFinalizeTimeSeconds,
            @JsonProperty("deleteBucketsAfterFinalize") Boolean deleteBucketsAfterFinalize,
            @JsonProperty("dlqName") Optional<QueueName> dlqName) {

        this.queueName = queueName;

        this.bucketSize = bucketSize == null ? DEFAULT_BUCKET_SIZE : bucketSize;
        this.maxDeliveryCount = maxDeliveryCount == null ? DEFAULT_MAX_DELIVERY_COUNT : maxDeliveryCount;

        this.repairWorkerPollSeconds = repairWorkerPollSeconds == null ? DEFAULT_REPAIR_POLL_SECONDS : repairWorkerPollSeconds;
        this.repairWorkerBucketFinalizeTimeSeconds =
                repairWorkerBucketFinalizeTimeSeconds == null ? DEFAULT_REPAIR_POLL_BUCKET_FINALIZE_SECONDS : repairWorkerBucketFinalizeTimeSeconds;

        this.deleteBucketsAfterFinalize = deleteBucketsAfterFinalize == null ? DEFAULT_DELETE_BUCKETS_ON_FINALIZE : deleteBucketsAfterFinalize;

        this.dlqName = dlqName;
    }
}
