package io.paradoxical.cassieq;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.paradoxical.cassieq.environment.SystemProps;

import java.util.Arrays;
import java.util.List;

public class WebConfiguration {
    @JsonProperty("allowedOrigins")
    private String allowedOriginsRaw = SystemProps.instance().CORS_ALLOWED_ORIGINS();

    @JsonIgnore
    public List<String> getAllowedOrigins() {
        return Arrays.asList(allowedOriginsRaw.split(","));
    }
}
