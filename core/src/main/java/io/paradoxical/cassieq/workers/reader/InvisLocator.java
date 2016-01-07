package io.paradoxical.cassieq.workers.reader;

import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.Message;
import org.joda.time.Duration;

import java.util.Optional;

public interface InvisLocator {
    Optional<Message> tryConsumeNextVisibleMessage(InvisibilityMessagePointer pointer, Duration invisiblity);
}
