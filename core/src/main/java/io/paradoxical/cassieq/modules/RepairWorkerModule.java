package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.netflix.governator.guice.lazy.LazySingleton;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import io.paradoxical.cassieq.modules.annotations.GenericScheduler;
import io.paradoxical.cassieq.modules.annotations.RepairPool;
import io.paradoxical.cassieq.workers.repair.RepairWorker;
import io.paradoxical.cassieq.workers.repair.RepairWorkerImpl;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;
import io.paradoxical.cassieq.workers.repair.SimpleRepairWorkerManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class RepairWorkerModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(RepairWorker.class, RepairWorkerImpl.class)
                        .build(RepairWorkerFactory.class));

        bind(RepairWorkerManager.class).to(SimpleRepairWorkerManager.class).in(LazySingleton.class);
    }

    @Singleton
    @RepairPool
    @Provides
    public ScheduledExecutorService poolScheduler() {
        return Executors.newScheduledThreadPool(10);
    }

    @Singleton
    @GenericScheduler
    @Provides
    public ScheduledExecutorService genericScheduler() {
        return Executors.newScheduledThreadPool(10);
    }
}
