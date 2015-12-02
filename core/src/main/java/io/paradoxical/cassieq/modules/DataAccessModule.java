package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import io.paradoxical.cassieq.dataAccess.EchoRepo;
import io.paradoxical.cassieq.dataAccess.interfaces.DbRepo;

public class DataAccessModule extends AbstractModule {

    @Override protected void configure() {
        bind(DbRepo.class).to(EchoRepo.class);
    }
}
