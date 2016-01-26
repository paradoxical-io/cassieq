package io.paradoxical.cassieq.unittests;

import io.paradoxical.cassieq.unittests.server.TestService;
import io.paradoxical.common.test.junit.RetryRule;
import io.paradoxical.common.test.logging.TestLoggingInitializer;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
@Ignore("for local testing")
public class YamlConfigTest {

    private final String configPath;

    // retry each test up to 3 times if they fail
    @Rule
    public RetryRule retry = new RetryRule(3);

    @Parameterized.Parameters(name = "{0}")
    public static List<String[]> getFiles() {
        final String[][] objects = {
                { "docker/data/conf/configuration.yml" }
        };

        return Arrays.asList(objects);
    }

    public YamlConfigTest(String configPath) {
        TestLoggingInitializer.init();

        this.configPath = configPath;
    }

    @Test
    public void test_yaml_files() throws Exception {
        final TestService serviceApplication = new TestService(Collections.emptyList());

        serviceApplication.run("server", configPath);

        serviceApplication.stop();
    }
}