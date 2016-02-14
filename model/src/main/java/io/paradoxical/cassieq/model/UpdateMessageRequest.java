package io.paradoxical.cassieq.model;

import lombok.Value;

@Value
public class UpdateMessageRequest {
    /**
     * An optional new messsage to provide for the payload.  If the message is null the old message will be kept
     */
    String message;

    /**
     * The new invisibility time to set the message to. If you set this to 0 the message will be effectively
     * deferred to another client.
     */
    long newInvisibilitySeconds;
}
