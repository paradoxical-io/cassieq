package suites;

import categories.StressTests;
import categories.VerySlowTests;
import io.paradoxical.cassieq.unittests.tests.TimeTests;
import io.paradoxical.cassieq.unittests.tests.YamlConfigTest;
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
@Categories.IncludeCategory(VerySlowTests.class)
@Suite.SuiteClasses({
     ParallelWorkerTests.class,
     StandardApiTests.class,
     MessageRepositoryTester.class,
     PopReceiptTester.class,
     QueueDeleterTests.class,
     ReaderTester.class,
     RepairTests.class,
     TimeTests.class,
     YamlConfigTest.class,
     QueueRepositoryTester.class
})
public class VerySlowTestsSuite {
}
