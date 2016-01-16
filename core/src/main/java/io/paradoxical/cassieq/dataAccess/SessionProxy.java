package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.WriteType;
import com.datastax.driver.core.exceptions.UnavailableException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.StopStrategy;
import com.github.rholder.retry.WaitStrategies;
import com.github.rholder.retry.WaitStrategy;
import io.paradoxical.cassieq.configurations.cassandra.CassandraConfiguration;
import io.paradoxical.cassieq.configurations.cassandra.CompareAndSetRetryConfig;
import lombok.experimental.Delegate;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SessionProxy implements Session {
    private interface ExecuteExclude {
        ResultSet execute(Statement statement);
    }

    @Delegate(excludes = ExecuteExclude.class)
    private final Session session;

    private final CassandraConfiguration configuration;

    public SessionProxy(final Session session, CassandraConfiguration configuration) {
        this.session = session;

        this.configuration = configuration;
    }

    @Override
    public ResultSet execute(final Statement statement) {
        final CompareAndSetRetryConfig casConfig = configuration.getCasConfig();

        final WaitStrategy waitStrategy = WaitStrategies.exponentialWait(casConfig.getMaxWaitTimeMs(), TimeUnit.MILLISECONDS);
        final StopStrategy stopStrategy = StopStrategies.stopAfterAttempt(casConfig.getMaxRetries());

        final Retryer<ResultSet> retrier =
                RetryerBuilder.<ResultSet>newBuilder()
                        .retryIfException(ex -> {
                            if (ex instanceof WriteTimeoutException) {
                                return ((WriteTimeoutException) ex).getWriteType() == WriteType.CAS;
                            }

                            return ex instanceof UnavailableException;
                        })
                        .withWaitStrategy(waitStrategy)
                        .withStopStrategy(stopStrategy)
                        .build();

        try {
            return retrier.call(() -> session.execute(statement));
        }
        catch (ExecutionException | RetryException e) {
            throw new RuntimeException("Error executing session retry!", e);
        }
    }
}
