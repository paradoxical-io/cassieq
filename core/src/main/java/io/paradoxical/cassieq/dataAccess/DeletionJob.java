package io.paradoxical.cassieq.dataAccess;

import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.QueueName;
import lombok.Data;

@Data
public class DeletionJob {
    private final QueueName queueName;

    private final int version;

    private final BucketSize bucketSize;
}
