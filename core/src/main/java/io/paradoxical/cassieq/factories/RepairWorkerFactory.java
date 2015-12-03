package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.workers.RepairWorker;

public interface RepairWorkerFactory {
    RepairWorker forQueue(QueueDefinition definition);
}
