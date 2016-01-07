package io.paradoxical.cassieq.modules;

import com.google.inject.Module;

import java.util.Arrays;
import java.util.List;

public class DefaultApplicationModules {
    public static List<Module> getModules() {
        return Arrays.asList(
                new LeadershipModule(),
                new MessageDeletionModule(),
                new JsonMapperModule(),
                new DataAccessModule(),
                new SessionProviderModule(),
                new RepairWorkerModule(),
                new ReaderModule(),
                new ConfigProviderModule(),
                new ClockModule());
    }
}
