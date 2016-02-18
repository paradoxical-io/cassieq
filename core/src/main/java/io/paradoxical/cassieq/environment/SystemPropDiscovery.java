package io.paradoxical.cassieq.environment;

import lombok.Value;

@Value
public class SystemPropDiscovery {
    String help;
    Object defaultValue;
    Object envVarName;
    Object currentValue;
}
