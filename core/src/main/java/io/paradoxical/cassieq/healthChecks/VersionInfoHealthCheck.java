package io.paradoxical.cassieq.healthChecks;

import com.hubspot.dropwizard.guice.InjectableHealthCheck;
import com.jcabi.manifests.Manifests;

public class VersionInfoHealthCheck extends InjectableHealthCheck {
    @Override protected Result check() throws Exception {
        try {
            return Result.healthy("Built branch: %s, Git Hash: %s",
                                  Manifests.read("SCM-Branch"),
                                  Manifests.read("SCM-Revision"));
        }
        catch (Exception ex) {
            return Result.healthy("Running in local develop. " +
                                  "To test a manifest run a full package " +
                                  "or add the info to your local manifests");
        }
    }

    @Override public String getName() {
        return "version-info";
    }
}
