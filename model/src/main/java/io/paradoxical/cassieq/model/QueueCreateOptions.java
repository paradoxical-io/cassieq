package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QueueCreateOptions {
    private static final Integer DEFAULT_BUCKET_SIZE = 20;
    private static final Integer DEFAULT_MAX_DEVLIVERY_COUNT = 5;


    @NotNull
    private final QueueName queueName;

    private final Integer bucketSize;

    private final Integer maxDeliveryCount;

    public QueueCreateOptions(QueueName queueName) {
        this(queueName, DEFAULT_BUCKET_SIZE, 5);
    }

    @JsonCreator
    public QueueCreateOptions(
            @JsonProperty("queueName") QueueName queueName,
            @JsonProperty("bucketSize") Integer bucketSize,
            @JsonProperty("maxDeliveryCount") Integer maxDeliveryCount) {

        this.queueName = queueName;

        this.bucketSize = bucketSize == null ? DEFAULT_BUCKET_SIZE : bucketSize;
        this.maxDeliveryCount = maxDeliveryCount == null ? DEFAULT_MAX_DEVLIVERY_COUNT : maxDeliveryCount;
    }
}
