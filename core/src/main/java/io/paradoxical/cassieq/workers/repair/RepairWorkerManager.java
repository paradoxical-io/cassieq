package io.paradoxical.cassieq.workers.repair;

public interface RepairWorkerManager {
    void start();

    void stop();

    default void notifyChanges() {
        start();
    }
}
