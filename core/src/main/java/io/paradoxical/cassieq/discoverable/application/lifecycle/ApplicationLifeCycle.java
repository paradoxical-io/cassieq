package io.paradoxical.cassieq.discoverable.application.lifecycle;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;

import static com.godaddy.logging.LoggerFactory.getLogger;


@Singleton
@SuppressWarnings("unused")
public class ApplicationLifeCycle implements Managed {
    private static final Logger logger = getLogger(ApplicationLifeCycle.class);

    private RepairWorkerManager repairWorkerManager;

    @Inject
    public ApplicationLifeCycle(RepairWorkerManager repairWorkerManager) {
        this.repairWorkerManager = repairWorkerManager;
    }

    @Override public void start() throws Exception {
        logger.info("Starting manager");

        repairWorkerManager.start();

        logger.success("Started!");
    }

    @Override public void stop() throws Exception {
        repairWorkerManager.stop();

        logger.dashboard("STOPPED");
    }
}