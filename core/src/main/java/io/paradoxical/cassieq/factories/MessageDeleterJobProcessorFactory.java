package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.dataAccess.DeletionJob;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageDeleterJobProcessor;

public interface MessageDeleterJobProcessorFactory {
    MessageDeleterJobProcessor createDeletionProcessor(DeletionJob job);
}
