package io.paradoxical.cassieq.configurations;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

import java.time.Duration;

public class RepairConfig {

    @NotEmpty
    @Getter
    @Setter
    private Integer managerRefreshRateSeconds = Long.valueOf(Duration.ofMinutes(1).getSeconds()).intValue();
}