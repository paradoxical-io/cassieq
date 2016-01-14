package io.paradoxical.cassieq.clustering.eventing;

public abstract class EventListener<T> {
    public abstract void onMessage(T item);
}
