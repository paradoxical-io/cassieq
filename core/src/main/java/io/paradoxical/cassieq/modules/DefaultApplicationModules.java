package io.paradoxical.cassieq.modules;

import com.google.inject.Module;
import io.paradoxical.cassieq.modules.auth.AuthModule;

import java.util.Arrays;
import java.util.List;

public class DefaultApplicationModules {
    public static List<Module> getModules() {
        return Arrays.asList(
                new MessagePublisherModule(),
                new LeadershipModule(),
                new ClusteringModule(),
                new MessageDeletionModule(),
                new QueueDeletionModule(),
                new JsonMapperModule(),
                new DataAccessModule(),
                new SessionProviderModule(),
                new RepairWorkerModule(),
                new ReaderModule(),
                new ResourceAllocationModule(),
                new EventingModule(),
                new ConfigProviderModule(),
                new ClockModule(),
                new AuthModule());
    }
}
