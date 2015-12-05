package io.paradoxical.cassieq.workers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BucketConfiguration {
    private Duration repairWorkerTimeout = Duration.standardSeconds(20);

    private Duration repairWorkerPollFrequency = Duration.standardSeconds(1);

    private boolean deleteBucketsAfterRepair = true;
}
