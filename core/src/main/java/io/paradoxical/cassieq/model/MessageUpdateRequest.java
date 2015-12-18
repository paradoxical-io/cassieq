package io.paradoxical.cassieq.model;

import lombok.Data;
import org.joda.time.Duration;

@Data
public class MessageUpdateRequest {
    private final Duration invisibilityDuration;

    private final MessageTag tag;

    private final int version;

    private final MonotonicIndex index;

    private String newBlob;

    public static MessageUpdateRequest from(UpdateMessageRequest request) {
        final PopReceipt popReceipt = PopReceipt.valueOf(request.getPopReceipt());

        final MessageUpdateRequest messageUpdateRequest =
                new MessageUpdateRequest(Duration.standardSeconds(request.newInvisibilitySeconds),
                                         popReceipt.getMessageTag(),
                                         popReceipt.getMessageVersion(),
                                         popReceipt.getMessageIndex());

        messageUpdateRequest.setNewBlob(request.getMessage());

        return messageUpdateRequest;
    }
}
