package io.paradoxical.cassieq.unittests.time;

import io.paradoxical.cassieq.model.Clock;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.Comparator;
import java.util.PriorityQueue;


public class TestClock implements Clock {

    public static TestClock create() {
        return new TestClock();
    }

    private Instant time = Instant.now();

    @Value
    @EqualsAndHashCode
    private static class SleepItem {
        Instant sleepUntil;
        Object notificationObject;
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

    public void tickSeconds(Long seconds){
        time = time.plus(Duration.standardSeconds(seconds));

        SleepItem peek;
        while ((peek = sleepItems.peek()) != null && peek.getSleepUntil().isBefore(now())){
            final Object notificationObject = peek.getNotificationObject();
            synchronized (notificationObject){
                notificationObject.notify();
            }
            sleepItems.remove();
        }
    }

    @Override
    public Instant now() {
        return time;
    }

    @Override
    public void sleepFor(final Duration duration) throws InterruptedException {
        final Object sleepObject = new Object();

        sleepItems.add(new SleepItem(now().plus(duration), sleepObject));

        synchronized (sleepObject){
            sleepObject.wait();
        }
    }
}
