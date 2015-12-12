package io.paradoxical.cassieq.workers.repair;

import io.paradoxical.cassieq.model.QueueDefinition;
import lombok.Data;

@Data
/**
 * Key that stores the worker and definition
 * but is hashable and identified by the definition
 */
class RepairWorkerKey {
    private QueueDefinition getQueueDefinition() {
        return repairWorker.forDefinition();
    }

    private final RepairWorker repairWorker;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepairWorkerKey)) {
            return false;
        }

        final RepairWorkerKey that = (RepairWorkerKey) o;

        return !(getQueueDefinition() != null ? !getQueueDefinition().equals(that.getQueueDefinition()) : that.getQueueDefinition() != null);

    }

    @Override
    public int hashCode() {
        return getQueueDefinition() != null ? getQueueDefinition().hashCode() : 0;
    }
}
