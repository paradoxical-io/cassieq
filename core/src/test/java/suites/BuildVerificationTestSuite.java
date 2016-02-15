package suites;

import categories.BuildVerification;
import categories.VerySlowTests;
import io.paradoxical.cassieq.unittests.*;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Categories.class)
@Categories.IncludeCategory(BuildVerification.class)
@Categories.ExcludeCategory(VerySlowTests.class)
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
    ApiAuthenticationTests.class
})
public class BuildVerificationTestSuite {
}
