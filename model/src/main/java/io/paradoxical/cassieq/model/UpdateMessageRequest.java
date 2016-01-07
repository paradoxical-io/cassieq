package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class UpdateMessageRequest {
    String message;
    long newInvisibilitySeconds;

    @JsonCreator
    public UpdateMessageRequest(
            @JsonProperty("message") String message,
            @JsonProperty("newInvisibilitySeconds") long newInvisibilitySeconds) {
        this.message = message;
        this.newInvisibilitySeconds = newInvisibilitySeconds;
    }
}
