package io.paradoxical.cassieq.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.godaddy.logging.Logger;
import io.paradoxical.cassieq.model.QueueName;
import org.glassfish.jersey.server.ExtendedUriInfo;

import java.util.concurrent.ConcurrentHashMap;

import static com.codahale.metrics.MetricRegistry.name;
import static com.godaddy.logging.LoggerFactory.getLogger;

public class QueueTimerProvider {
    private static final Logger logger = getLogger(QueueTimerProvider.class);

    private final QueueTimer queueTimer;
    private final MetricRegistry registry;

    private final ConcurrentHashMap<QueueName, Timer> timers = new ConcurrentHashMap<>();

    public QueueTimerProvider(QueueTimer queueTimer, MetricRegistry registry) {
        this.queueTimer = queueTimer;
        this.registry = registry;
    }


    public Timer.Context startTimer(final ExtendedUriInfo uriInfo) {
        try {
            final String queueFromPathName = uriInfo.getPathSegments(queueTimer.segment()).get(0).getPath();

            final QueueName queueName = QueueName.valueOf(queueFromPathName);

            if (timers.contains(queueName)) {
                return timers.get(queueName).time();
            }

            final Timer timer = registry.timer(name("queue", queueTimer.actionName(), queueName.get()));

            timers.put(queueName, timer);

            return timer.time();
        }
        catch (Exception ex) {
            logger.error(ex, "Unable to make timer!");

            return new Timer().time();
        }
    }
}
