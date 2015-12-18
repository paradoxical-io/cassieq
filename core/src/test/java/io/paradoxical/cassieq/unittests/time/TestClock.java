package io.paradoxical.cassieq.unittests.time;

import io.paradoxical.cassieq.model.time.Clock;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.Calendar;
import java.util.Comparator;
import java.util.PriorityQueue;


public class TestClock implements Clock {

    public static TestClock create() {
        return new TestClock();
    }

    private Instant time = truncate(Instant.now());

    private final Thread heartbeatThread;

    private volatile boolean running = true;

    @Value
    @EqualsAndHashCode
    private static class SleepItem {
        Instant sleepUntil;
        Object notificationObject;
    }

    public TestClock() {
        heartbeatThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(17);
                }
                catch (InterruptedException e) {
                    return;
                }

                tickSeconds(0L);
            }
        });
        heartbeatThread.setDaemon(true);

        heartbeatThread.start();
    }

    protected final void finalize() {
        running = false;
    }

    private final PriorityQueue<SleepItem> sleepItems = new PriorityQueue<>(new Comparator<SleepItem>() {
        @Override
        public int compare(final SleepItem o1, final SleepItem o2) {
            return o1.getSleepUntil().compareTo(o2.getSleepUntil());
        }
    });

    public void tick() {
        tickSeconds(1L);
    }

    public synchronized void tickSeconds(Long seconds) {
        time = time.plus(Duration.standardSeconds(seconds));

        SleepItem peek;
        while ((peek = sleepItems.peek()) != null && shouldExecute(sleepItems.peek())) {
            final Object notificationObject = peek.getNotificationObject();
            synchronized (notificationObject) {
                notificationObject.notify();
            }
            sleepItems.remove();
        }
    }

    private boolean shouldExecute(final SleepItem peek) {
        final Instant now = now();

        return truncate(peek.getSleepUntil()).isBefore(now) ||
               truncate(peek.getSleepUntil()).isEqual(now);
    }

    @Override
    public Instant now() {
        return truncate(time);
    }

    @Override
    public void sleepFor(final Duration duration) throws InterruptedException {
        final Object sleepObject = new Object();

        synchronized (this) {
            sleepItems.add(new SleepItem(now().plus(duration), sleepObject));
        }

        synchronized (sleepObject) {
            sleepObject.wait();
        }
    }

    @Override
    public void sleepTill(final Instant instant) throws InterruptedException {
        final Object sleepObject = new Object();


        if (Instant.now().isAfter(truncate(instant))) {
            return;
        }

        synchronized (this) {
            sleepItems.add(new SleepItem(truncate(instant), sleepObject));
        }

        synchronized (sleepObject) {
            sleepObject.wait();
        }
    }

    private Instant truncate(final Instant instant) {
        return new Instant(DateUtils.truncate(instant.toDate(), Calendar.SECOND).toInstant().toEpochMilli());
    }

    @Override
    public long jitter(final int i) {
        return 0;
    }
}
