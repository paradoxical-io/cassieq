package suites;

import categories.VerySlowTests;
import io.paradoxical.cassieq.unittests.*;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(VerySlowTests.class)
@Suite.SuiteClasses({
    SlowTests.class,
    ApiTester.class,
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
