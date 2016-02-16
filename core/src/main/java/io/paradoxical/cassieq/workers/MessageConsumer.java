package io.paradoxical.cassieq.workers;

import io.paradoxical.cassieq.model.Message;
import org.joda.time.Duration;

import java.util.Optional;

public interface MessageConsumer {
    Optional<Message> tryConsume(Message message, Duration invisiblity);
}
