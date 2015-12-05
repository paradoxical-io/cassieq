package io.paradoxical.cassieq.factories;

import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.workers.repair.RepairWorker;

public interface RepairWorkerFactory {
    RepairWorker forQueue(QueueDefinition definition);
}
