package io.paradoxical.cassieq.unittests;

import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.api.client.CassieqApi;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;
import org.junit.Ignore;
import org.junit.Test;
import retrofit.Response;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Random;

import static com.godaddy.logging.LoggerFactory.getLogger;

@Ignore("Stress tests")
public class StressTester extends TestBase {
    private static final Logger logger = getLogger(StressTester.class);

    public static final AccountName testAccountName = AccountName.valueOf("stress-testing");

    @Ignore
    @Test
    public void stress_test() throws InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        final CassieqApi client = CassieqApi.createClient("http://localhost:8080", getTestAccountCredentials(getDefaultInjector()));

        final QueueName perftest = QueueName.valueOf("perftest1");

        for (int i = 0; i < 50; i++) {
            startWriter(client, perftest);
        }

//        for (int i = 0; i < 5; i++) {
//            startReader(client, perftest);
//        }

        Thread.sleep(Duration.ofDays(1).toMillis());
    }

    private void startReader(final CassieqApi client, final QueueName perftest) {
        new Thread(() -> {
            while (true) {
                try {
                    final Response<GetMessageResponse> execute = client.getMessage(testAccountName, perftest, 30L).execute();

                    final String popReceipt = execute.body().getPopReceipt();

                    client.ackMessage(testAccountName, perftest, popReceipt).execute();
                }
                catch (IOException e) {
                    logger.error(e, "Error");
                }
            }
        }).start();
    }

    private void startWriter(final CassieqApi client, final QueueName perftest) {
        new Thread(() -> {
            final Random random = new Random();
            while (true) {
                try {
                    client.addMessage(testAccountName, perftest, random.nextInt()).execute();
                }
                catch (IOException e) {
                    logger.error(e, "Error");
                }
            }
        }).start();
    }
}
