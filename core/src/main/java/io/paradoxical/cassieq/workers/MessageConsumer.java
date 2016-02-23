package io.paradoxical.cassieq.workers;

import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.workers.reader.ConsumableMessage;

import java.util.Optional;

public interface MessageConsumer {
    Optional<Message> tryConsume(ConsumableMessage consumableMessage);

    interface Factory {
        MessageConsumer forQueue(QueueDefinition queueDefinition);
    }
}
