package io.paradoxical.cassieq.workers;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageRepository;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.QueueDefinition;
import org.joda.time.Duration;

import java.util.Optional;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class MessageConsumer {
    private Logger logger = getLogger(MessageConsumer.class);

    private final QueueDefinition queueDefinition;

    private final MessageRepository messageRepository;

    @Inject
    public MessageConsumer(
            MessageRepoFactory messageRepoFactory,
            @Assisted QueueDefinition definition) {
        messageRepository = messageRepoFactory.forQueue(definition);

        this.queueDefinition = definition;

        logger = logger.with("q", definition.getQueueName()).with("version", definition.getVersion());
    }

    /**
     * Consumes with business logic
     * @param message
     * @param invisiblity
     * @return
     */
    public Optional<Message> tryConsume(Message message, Duration invisiblity) {
        if (!messageConsumable(message)) {
            return Optional.empty();
        }

        return messageRepository.rawConsumeMessage(message, invisiblity);
    }

    /**
     * Determines if the message is allowed to be consumed
     *
     * @param message
     * @return
     */
    private boolean messageConsumable(final Message message) {
        if (message.getDeliveryCount() >= queueDefinition.getMaxDeliveryCount()) {
            logger.with("tag", message.getTag())
                  .with("already-delivered-count", message.getDeliveryCount())
                  .with("max-delivery-count", queueDefinition.getMaxDeliveryCount())
                  .warn("Message exceeded delivery count, acking and moving on");

            if (ackMessage(message)) {
                logger.with("tag", message.getTag()).success("Acked dead message");
            }

            return false;
        }

        return true;
    }


    private boolean ackMessage(final Message message) {
        return messageRepository.ackMessage(message);
    }

    public interface Factory {
        MessageConsumer forQueue(QueueDefinition queueDefinition);
    }
}
