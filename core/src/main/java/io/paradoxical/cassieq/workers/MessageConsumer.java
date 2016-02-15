package io.paradoxical.cassieq.workers;

import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.QueueDefinition;
import org.joda.time.Duration;

import java.util.Optional;

public interface MessageConsumer {
    Optional<Message> tryConsume(Message message, Duration invisiblity);


    interface Factory {
        MessageConsumer forQueue(QueueDefinition queueDefinition);
    }
}
