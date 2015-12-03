package io.paradoxical.cassieq.modules;

import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import io.paradoxical.cassieq.modules.annotations.RepairPool;
import io.paradoxical.cassieq.workers.RepairWorker;
import io.paradoxical.cassieq.workers.RepairWorkerImpl;
import io.paradoxical.cassieq.workers.RepairWorkerManager;
import io.paradoxical.cassieq.workers.SimpleRepairWorkerManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class RepairWorkerModule extends AbstractModule {

    @Override protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(RepairWorker.class, RepairWorkerImpl.class)
                        .build(RepairWorkerFactory.class));

        bind(RepairWorkerManager.class).to(SimpleRepairWorkerManager.class).in(Singleton.class);
    }

    @Singleton
    @RepairPool
    @Provides
    public ScheduledExecutorService executorService(){
        return Executors.newScheduledThreadPool(10);
    }
}
