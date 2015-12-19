package io.paradoxical.cassieq.configurations;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.Duration;

public class RepairConfig {

    @NotNull
    @Getter
    @Setter
    private Integer managerRefreshRateSeconds = Long.valueOf(Duration.ofMinutes(1).getSeconds()).intValue();
}