package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class UpdateMessageResponse {
    String popReceipt;
    MessageTag messageTag;

    @JsonCreator
    public UpdateMessageResponse(
            @JsonProperty("popReceipt") String popReceipt,
            @JsonProperty("messageTag") MessageTag messageTag) {

        this.popReceipt = popReceipt;
        this.messageTag = messageTag;
    }
}
