package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.DeletionJob;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageDeletorJobProcessor;

public interface MessageDeleterJobProcessorFactory {
    MessageDeletorJobProcessor createDeletionProcessor(DeletionJob job);
}
