package io.paradoxical.cassieq.workers.reader;

import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.PopReceipt;
import org.joda.time.Duration;

import java.util.Optional;

public interface Reader {
    Optional<Message> nextMessage(Duration invisiblity);

    boolean ackMessage(PopReceipt popReceipt);
}
