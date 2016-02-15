package io.paradoxical.cassieq.unittests.tests;

import categories.BuildVerification;
import io.paradoxical.cassieq.clustering.eventing.EventBus;
import io.paradoxical.cassieq.clustering.eventing.EventListener;
import io.paradoxical.cassieq.model.events.QueueAddedEvent;
import io.paradoxical.cassieq.unittests.TestBase;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class EventBusTests extends TestBase {
    @Test
    public void events_register_and_consume() throws InterruptedException {
        final EventBus eventBus = getDefaultInjector().getInstance(EventBus.class);

        Semaphore lock = new Semaphore(1);

        lock.acquire();

        eventBus.register(QueueAddedEvent.class, new EventListener<QueueAddedEvent>() {
            @Override
            public void onMessage(final QueueAddedEvent item) {
                lock.release();
            }
        });

        eventBus.publish(new QueueAddedEvent());

        if (!lock.tryAcquire(10, TimeUnit.SECONDS)) {
            fail("condition never signaled");
        }
    }

    @Test
    public void events_unregister() throws InterruptedException {
        final EventBus eventBus = getDefaultInjector().getInstance(EventBus.class);

        Semaphore lock = new Semaphore(1);

        AtomicLong count = new AtomicLong(0);

        lock.acquire();

        String listenerId = eventBus.register(QueueAddedEvent.class, new EventListener<QueueAddedEvent>() {
            @Override
            public void onMessage(final QueueAddedEvent item) {
                count.incrementAndGet();

                lock.release();
            }
        });

        eventBus.publish(new QueueAddedEvent());

        if (!lock.tryAcquire(10, TimeUnit.SECONDS)) {
            fail("condition never signaled");
        }

        eventBus.unregister(listenerId);

        eventBus.publish(new QueueAddedEvent());

        if (lock.tryAcquire(50, TimeUnit.MILLISECONDS)) {
            fail("Should not have been signaled");
        }

        assertThat(count.get()).isEqualTo(1);
    }
}
