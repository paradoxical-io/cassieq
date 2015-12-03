package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class QueueCreateOptions {
    @NotNull
    private final QueueName queueName;

    private final Integer bucketSize;

    private final Integer maxDevliveryCount;

    public QueueCreateOptions(QueueName queueName) {
        this(queueName, 20, 5);
    }

    @JsonCreator
    public QueueCreateOptions(
            @JsonProperty("queueName") QueueName queueName,
            @JsonProperty("bucketSize") Integer bucketSize,
            @JsonProperty("maxDevliveryCount") Integer maxDevliveryCount) {

        this.queueName = queueName;

        this.bucketSize = bucketSize;
        this.maxDevliveryCount = maxDevliveryCount;
    }
}
