package io.paradoxical.cassieq.configurations;

import io.paradoxical.cassieq.environment.SystemProps;
import lombok.Data;

@Data
public class LogConfig {
    private Boolean logRawJerseyRequests = SystemProps.instance().LOG_RAW_REQUESTS();
}
