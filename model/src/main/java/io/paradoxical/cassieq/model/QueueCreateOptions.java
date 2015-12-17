package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class QueueCreateOptions {
    private static final Integer DEFAULT_BUCKET_SIZE = 20;
    private static final Integer DEFAULT_MAX_DELIVERY_COUNT = 5;
    private static final Integer DEFAULT_REPAIR_POLL_SECONDS = 5;
    private static final Integer DEFAULT_REPAIR_POLL_BUCKET_FINALIZE_SECONDS = 15;
    private static final Integer DEFAULT_MAX_DELIVERY = 5;
    private static final Boolean DEFAULT_DELETE_BUCKETS_ON_FINALIZE = true;

    @NotNull
    private final QueueName queueName;

    private final Integer bucketSize;

    private final Integer maxDeliveryCount;

    private final Integer repairWorkerPollSeconds;

    private final Integer repairWorkerBucketFinalizeTimeSeconds;

    private final Boolean deleteBucketsAfterFinalize;

    public QueueCreateOptions(QueueName queueName) {
        this(queueName,
             DEFAULT_BUCKET_SIZE,
             DEFAULT_MAX_DELIVERY_COUNT,
             DEFAULT_REPAIR_POLL_SECONDS,
             DEFAULT_REPAIR_POLL_BUCKET_FINALIZE_SECONDS,
             DEFAULT_DELETE_BUCKETS_ON_FINALIZE);
    }

    @JsonCreator
    public QueueCreateOptions(
            @JsonProperty("queueName") QueueName queueName,
            @JsonProperty("bucketSize") Integer bucketSize,
            @JsonProperty("maxDeliveryCount") Integer maxDeliveryCount,
            @JsonProperty("repairWorkerPollSeconds") Integer repairWorkerPollSeconds,
            @JsonProperty("repairWorkerBucketFinalizeTimeSeconds") Integer repairWorkerBucketFinalizeTimeSeconds,
            @JsonProperty("deleteBucketsAfterFinalize") Boolean deleteBucketsAfterFinalize) {

        this.queueName = queueName;

        this.bucketSize = bucketSize == null ? DEFAULT_BUCKET_SIZE : bucketSize;
        this.maxDeliveryCount = maxDeliveryCount == null ? DEFAULT_MAX_DELIVERY_COUNT : maxDeliveryCount;

        this.repairWorkerPollSeconds = repairWorkerPollSeconds == null ? DEFAULT_REPAIR_POLL_SECONDS : repairWorkerPollSeconds;
        this.repairWorkerBucketFinalizeTimeSeconds =
                repairWorkerBucketFinalizeTimeSeconds == null ? DEFAULT_REPAIR_POLL_BUCKET_FINALIZE_SECONDS : repairWorkerBucketFinalizeTimeSeconds;

        this.deleteBucketsAfterFinalize = deleteBucketsAfterFinalize == null ? DEFAULT_DELETE_BUCKETS_ON_FINALIZE : deleteBucketsAfterFinalize;
    }
}
