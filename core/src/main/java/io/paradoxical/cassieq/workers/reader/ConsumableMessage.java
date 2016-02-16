package io.paradoxical.cassieq.workers.reader;

import io.paradoxical.cassieq.model.Message;
import lombok.Builder;
import lombok.Value;
import org.joda.time.Duration;

@Value
@Builder(toBuilder = true)
public class ConsumableMessage {
    Message message;

    Duration invisibility;

    Source source;
}