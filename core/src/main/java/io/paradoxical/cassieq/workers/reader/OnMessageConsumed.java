package io.paradoxical.cassieq.workers.reader;

import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.QueueDefinition;
import org.joda.time.Duration;

@FunctionalInterface
public interface OnMessageConsumed {
    /**
     * Mark the message was consumed
     *
     * @param message
     * @param invisiblity
     */
    void markConsumed(Message message, Duration invisiblity);

    interface OnMessageConsumedFactory {
        OnMessageConsumed forQueue(QueueDefinition definition);
    }
}
