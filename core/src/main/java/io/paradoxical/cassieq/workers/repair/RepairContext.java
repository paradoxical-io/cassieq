package io.paradoxical.cassieq.workers.repair;

import io.paradoxical.cassieq.model.RepairBucketPointer;
import lombok.Data;
import org.joda.time.DateTime;

@Data
public class RepairContext {
    private final RepairBucketPointer pointer;

    private final DateTime tombstonedAt;
}
