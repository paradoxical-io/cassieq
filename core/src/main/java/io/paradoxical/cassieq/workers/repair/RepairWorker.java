package io.paradoxical.cassieq.workers.repair;

import io.paradoxical.cassieq.model.QueueDefinition;

public interface RepairWorker {
    void start();

    void stop();

    QueueDefinition forDefinition();
}
