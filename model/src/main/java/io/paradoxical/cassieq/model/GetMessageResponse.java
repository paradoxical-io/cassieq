package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class GetMessageResponse {
    String popReceipt;

    String message;

    int deliveryCount;

    MessageTag messageTag;

    @JsonCreator
    public GetMessageResponse(
            @JsonProperty("popReceipt") String popReceipt,
            @JsonProperty("message") String message,
            @JsonProperty("deliveryCount") int deliveryCount,
            @JsonProperty("messageTag") MessageTag messageTag) {

        this.popReceipt = popReceipt;
        this.message = message;
        this.deliveryCount = deliveryCount;
        this.messageTag = messageTag;
    }
}
