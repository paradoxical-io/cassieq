package io.paradoxical.cassieq.workers.reader;

import io.paradoxical.cassieq.model.Message;
import org.joda.time.Duration;

import java.util.Optional;

public interface InvisStrategy {
    /**
     * Find and consume a newly visible message
     *
     * @param invisiblity
     * @return
     */
    Optional<Message> tryConsumeNextVisibleMessage(Duration invisiblity);

    void onMessageConsumed(Message message, Duration invisiblity);
}

