package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.interfaces.MessageRepository;
import io.paradoxical.cassieq.model.QueueDefinition;

public interface MessageRepoFactory {
    MessageRepository forQueue(QueueDefinition definition);
}
