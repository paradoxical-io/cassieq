package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.paradoxical.cassieq.ServiceConfiguration;
import org.jgroups.JChannel;

import java.io.File;

public class LeadershipModule extends AbstractModule {

    @Override protected void configure() {
    }


    @Provides
    private JChannel createJChannel(final ServiceConfiguration config) {
        try {
            String raftConfigPath = new File(config.getRepairConf().getRaftConfigPath()).getCanonicalPath();
            return new JChannel(raftConfigPath);
        }
        catch (Exception excn) {
            throw new RuntimeException("Unable to configure JChannel", excn);
        }
    }

}