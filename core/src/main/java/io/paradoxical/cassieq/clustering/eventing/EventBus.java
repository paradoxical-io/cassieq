package io.paradoxical.cassieq.clustering.eventing;

public interface EventBus {
    <T extends Event> void publish(T  event);

    <T extends Event> void register(Class<T> eventType, EventListener<T> listener);
}

