package io.paradoxical.cassieq.workers.reader;

import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.QueueDefinition;
import org.joda.time.Duration;

import java.util.Optional;

public interface InvisStrategy {
    Optional<Message> findNextVisibleMessage(Duration invisiblity);

    void trackConsumedMessage(Message message);

    interface Factory {
        InvisStrategy forQueue(QueueDefinition definition);
    }
}
