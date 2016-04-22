package io.paradoxical.cassieq.workers;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.QueueDefinition;
import org.joda.time.Duration;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class DefaultMessagePublisher implements MessagePublisher {

    private static final Logger logger = getLogger(DefaultMessagePublisher.class);

    private final MonotonicRepoFactory monotonicRepository;
    private final MessageRepoFactory messageRepoFactory;

    @Inject
    public DefaultMessagePublisher(
            MonotonicRepoFactory monotonicRepository,
            MessageRepoFactory messageRepoFactory) {
        this.monotonicRepository = monotonicRepository;
        this.messageRepoFactory = messageRepoFactory;
    }

    @Override
    public void put(final QueueDefinition queueDefinition, final String message, final Long initialInvisibilityTimeSeconds) throws ExistingMonotonFoundException {
        final Message messageToInsert = Message.builder()
                                               .blob(message)
                                               .index(monotonicRepository.forQueue(queueDefinition.getId())
                                                                         .nextMonotonic())
                                               .build();

        final Duration initialInvisibility = Duration.standardSeconds(initialInvisibilityTimeSeconds);

        messageRepoFactory.forQueue(queueDefinition)
                          .putMessage(messageToInsert, initialInvisibility);

        logger.with("index", messageToInsert.getIndex())
              .with("tag", messageToInsert.getTag())
              .with("queue-id", queueDefinition.getId())
              .debug("Adding message");
    }
}
