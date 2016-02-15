package suites;

import categories.BuildVerification;
import categories.StressTests;
import categories.VerySlowTests;
import io.paradoxical.cassieq.unittests.tests.EventBusTests;
import io.paradoxical.cassieq.unittests.tests.TimeTests;
import io.paradoxical.cassieq.unittests.tests.YamlConfigTest;
import io.paradoxical.cassieq.unittests.tests.api.ApiAuthenticationTests;
import io.paradoxical.cassieq.unittests.tests.api.StandardApiTests;
import io.paradoxical.cassieq.unittests.tests.faultTolerance.RepairTests;
import io.paradoxical.cassieq.unittests.tests.queueSemantics.PopReceiptTester;
import io.paradoxical.cassieq.unittests.tests.queueSemantics.QueueDeleterTests;
import io.paradoxical.cassieq.unittests.tests.queueSemantics.ReaderTester;
import io.paradoxical.cassieq.unittests.tests.repos.MessageRepositoryTester;
import io.paradoxical.cassieq.unittests.tests.repos.QueueRepositoryTester;
import io.paradoxical.cassieq.unittests.tests.stress.ParallelWorkerTests;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Categories.class)
@Categories.IncludeCategory(BuildVerification.class)
@Categories.ExcludeCategory({ VerySlowTests.class, StressTests.class })
@Suite.SuiteClasses({
        StandardApiTests.class,
        MessageRepositoryTester.class,
        PopReceiptTester.class,
        QueueDeleterTests.class,
        ReaderTester.class,
        RepairTests.class,
        TimeTests.class,
        YamlConfigTest.class,
        QueueRepositoryTester.class,
        ApiAuthenticationTests.class,
        ParallelWorkerTests.class,
        EventBusTests.class
})
public class BuildVerificationTestSuite {
}
