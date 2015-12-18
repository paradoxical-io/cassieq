package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class UpdateMessageRequest {
    String popReceipt;
    String message;
    int newInvisibilitySeconds;

    @JsonCreator
    public UpdateMessageRequest(
            @JsonProperty("popReceipt") String popReceipt,
            @JsonProperty("message") String message,
            @JsonProperty("newInvisibilitySeconds") int newInvisibilitySeconds) {
        this.popReceipt = popReceipt;
        this.message = message;
        this.newInvisibilitySeconds = newInvisibilitySeconds;
    }
}
