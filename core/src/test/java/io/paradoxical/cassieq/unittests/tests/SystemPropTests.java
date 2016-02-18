package io.paradoxical.cassieq.unittests.tests;

import categories.BuildVerification;
import io.paradoxical.cassieq.environment.SystemPropDiscovery;
import io.paradoxical.cassieq.environment.SystemProps;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Category({ BuildVerification.class })
public class SystemPropTests {
    @Test
    public void env_var_help_command_works() throws Exception {
        final List<SystemPropDiscovery> discover = SystemProps.discover();

        assertThat(discover).isNotEmpty();

        discover.forEach(System.out::println);
    }
}
