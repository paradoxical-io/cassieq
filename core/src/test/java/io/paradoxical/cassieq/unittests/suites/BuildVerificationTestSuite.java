package io.paradoxical.cassieq.unittests.suites;

import categories.BuildVerification;
import io.paradoxical.cassieq.unittests.ApiTester;
import io.paradoxical.cassieq.unittests.MessageRepositoryTester;
import io.paradoxical.cassieq.unittests.PopReceiptTester;
import io.paradoxical.cassieq.unittests.QueueDeleterTests;
import io.paradoxical.cassieq.unittests.ReaderTester;
import io.paradoxical.cassieq.unittests.RepairTests;
import io.paradoxical.cassieq.unittests.TestBase;
import io.paradoxical.cassieq.unittests.TimeTests;
import io.paradoxical.cassieq.unittests.YamlConfigTest;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(BuildVerification.class)
@Suite.SuiteClasses({
    ApiTester.class,
    MessageRepositoryTester.class,
    PopReceiptTester.class,
    QueueDeleterTests.class,
    ReaderTester.class,
    RepairTests.class,
    TimeTests.class,
    YamlConfigTest.class
})
public class BuildVerificationTestSuite {
}
