package io.paradoxical.cassieq.workers.reader;

import io.paradoxical.cassieq.model.Message;
import org.joda.time.Duration;

public class InvisConsumerAdapter implements OnMessageConsumed {
    private final InvisStrategy strategy;

    public InvisConsumerAdapter(InvisStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void markConsumed(final Message message, final Duration invisiblity) {
        strategy.onMessageConsumed(message, invisiblity);
    }
}
